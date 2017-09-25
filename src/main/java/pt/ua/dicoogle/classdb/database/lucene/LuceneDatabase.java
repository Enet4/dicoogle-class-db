/**
 * Copyright (C) 2017 UA.PT Bioinformatics - http://bioinformatics.ua.pt
 *
 * This file is part of Dicoogle Classification Database (dicoogle-class-db).
 *
 * dicoogle-class-db is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * dicoogle-class-db is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.dicoogle.classdb.database.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.NumericUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.classdb.database.Database;
import pt.ua.dicoogle.classdb.database.DatabaseReader;
import pt.ua.dicoogle.classdb.database.DatabaseWriter;
import pt.ua.dicoogle.classdb.database.struct.DatabaseItem;
import pt.ua.dicoogle.classdb.database.struct.DatabaseItemImpl;
import pt.ua.dicoogle.classdb.database.struct.QueryParameters;
import pt.ua.dicoogle.classdb.database.util.ItemPrediction;
import pt.ua.dicoogle.classdb.database.util.RuntimeIOException;
import pt.ua.dicoogle.classdb.database.util.StreamUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class LuceneDatabase implements Database {
    private static final Logger logger = LoggerFactory.getLogger(LuceneDatabase.class);

    private static final String FIELD_URI = "uri";
    private static final String FIELD_CLASSIFICATION_ID = "id";
    private static final String FIELD_CLASSIFIER_NAME = "classifier";
    private static final String FIELD_CRITERION = "criterion";
    private static final String FIELD_PREDICTED_CLASS = "prediction";
    private static final String FIELD_PROBABILITY = "prob";
    private static final String FIELD_SCORE = "score";
    private static final String FIELD_CONTENTS = "contents";

    private final FSDirectory dir;
    private volatile DirectoryReader reader;

    public LuceneDatabase(Path dir) throws IOException {
        this.dir = FSDirectory.open(dir);
        this.reader = null;
    }

    @Override
    public void close() throws IOException {
        if (this.reader != null) {
            try {
                this.reader.close();
            } catch (IOException e) {
                logger.warn("Failed to close Lucene Database reader", e);
            }
        }
        this.dir.close();
    }

    @Override
    public DatabaseWriter createWriter() throws IOException {
        return new Writer();
    }

    protected synchronized IndexReader getUpdatedReader() throws IOException {
        if (this.reader == null) {
            this.reader = DirectoryReader.open(this.dir);
        } else {
            DirectoryReader r = DirectoryReader.openIfChanged(this.reader);
            if (r != null) {
                this.reader = r;
            }
        }
        return this.reader;
    }

    @Override
    public DatabaseReader createReader() throws IOException {
        return new Reader();
    }

    protected class Writer implements DatabaseWriter {
        private final IndexWriter writer;

        public Writer() throws IOException {
            Analyzer analyzer = new SimpleAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            config.setCommitOnClose(true);
            this.writer = new IndexWriter(LuceneDatabase.this.dir, config);
        }

        @Override
        public Writer add(DatabaseItem dbItem) throws IOException {
            String uri = dbItem.item().toString();
            String predId = dbItem.predictionIdentifier().toString();
            String classificationId = uri + '|' + predId;
            String classifierCrit = dbItem.classifierName() + '/' + dbItem.criterion();
            String pred = dbItem.predictedClass();
            String contents;
            if ("true".equals(pred)) {
                contents = dbItem.item().toString() + ' ' + dbItem.classifierName()
                        + ' ' + dbItem.criterion() + ' ' + classifierCrit;
            } else if ("false".equals(pred)) {
                contents = dbItem.item().toString() + ' ' + dbItem.classifierName();
            } else {
                contents = dbItem.item().toString() + ' ' + dbItem.classifierName()
                        + ' ' + dbItem.criterion() + ' ' + classifierCrit + ' ' + pred;
            }
            List<IndexableField> doc = Arrays.asList(
                    // classified item URI
                new StringField(FIELD_URI, uri, Field.Store.YES),
                    // classification ID (URI & prediction identifier)
                new StringField(FIELD_CLASSIFICATION_ID, classificationId, Field.Store.NO),
                    // classifier name
                new StringField(FIELD_CLASSIFIER_NAME, dbItem.classifierName(), Field.Store.YES),
                    // criterion (class family)
                new StringField(FIELD_CRITERION, dbItem.criterion(), Field.Store.YES),
                    // prediction
                new StringField(FIELD_PREDICTED_CLASS, dbItem.predictedClass(), Field.Store.YES),
                    // for "criterion:prediction" query support
                new StringField(dbItem.criterion(), dbItem.predictedClass(), Field.Store.NO),
                    // for "classifier/criterion:prediction" query support
                new StringField(classifierCrit, dbItem.predictedClass(), Field.Store.NO),
                    // concrete probability
                new StoredField(FIELD_PROBABILITY, dbItem.score()),
                    // integer-encoded probability, used as a score for sorting
                new SortedNumericDocValuesField(FIELD_SCORE, encodeScore(dbItem.score())),
                    // analysed text field with the whole content (for free text queries)
                new TextField(FIELD_CONTENTS, contents, Field.Store.NO)
            );
            writer.updateDocument(new Term(FIELD_CLASSIFICATION_ID, classificationId), doc);
            return this;
        }

        public long encodeScore(double score) {
            return NumericUtils.doubleToSortableLong(score);
        }

        @Override
        public boolean remove(URI item) throws IOException {

            Term uriTerm = new Term("uri", item.toString());
            try (IndexReader reader = DirectoryReader.open(this.writer, false, false)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs docs = searcher.search(new TermQuery(uriTerm), 1);
                if (docs.scoreDocs.length == 0) {
                    return false;
                }
                if (writer.tryDeleteDocument(reader, docs.scoreDocs[0].doc) != -1) {
                    return true;
                }
            }
            writer.deleteDocuments(uriTerm);
            return true;
        }

        @Override
        public void close() throws IOException {
            this.writer.close();
        }
    }

    protected class Reader implements DatabaseReader {
        private final IndexReader reader;
        private final IndexSearcher searcher;

        public Reader() throws IOException {
            this.reader = LuceneDatabase.this.getUpdatedReader();
            this.searcher = new IndexSearcher(reader);
        }

        @Override
        public Stream<DatabaseItem> search(String query, QueryParameters params) throws IOException {
            final Query q;
            try {
                q = createGenericQuery(query, params);
            } catch (QueryNodeException e) {
                throw new RuntimeIOException("Failed to parse query", e);
            }
            int n = params.getNumberOfResults();
            if (n == -1 || params.isOnlyBest()) {
                n = this.reader.maxDoc();
            }
            TopFieldCollector collector = TopFieldCollector.create(
                    new Sort(new SortedNumericSortField("score", SortField.Type.LONG, true)), n, true, true, false);

            this.searcher.search(q, collector);

            Stream<DatabaseItem> stream = Arrays.stream(collector.topDocs().scoreDocs)
                    .map(sd -> {
                        try {
                            return toItem(this.reader.document(sd.doc));
                        } catch (IOException e) {
                            throw new RuntimeIOException(e);
                        }
                    }).filter(item -> item.score() > params.getThreshold());
            if (params.isOnlyBest()) {
                stream = StreamUtil.firstOf(stream, ItemPrediction::from);
                if (n > 0) {
                    stream = stream.limit(n);
                }
            }
            return stream;
        }

        protected DatabaseItem toItem(Document doc) {
            String uri = doc.get(FIELD_URI);
            String classifier = doc.get(FIELD_CLASSIFIER_NAME);
            String criterion = doc.get(FIELD_CRITERION);
            String prediction = doc.get(FIELD_PREDICTED_CLASS);
            double prob = doc.getField(FIELD_PROBABILITY).numericValue().doubleValue();
            return new DatabaseItemImpl(URI.create(uri), classifier, criterion, prediction, prob);
        }

        protected Query createGenericQuery(String query, QueryParameters params) throws QueryNodeException {
            StandardQueryParser queryParserHelper = new StandardQueryParser();
            return queryParserHelper.parse(query, FIELD_CONTENTS);
        }

        protected Query createSimpleQuery(String query, QueryParameters params) {

            if (query.startsWith("uri:")) {
                try {
                    URI uri = new URI(sanitize(query.substring(4)));
                    return createEqualItem(uri);
                } catch (URISyntaxException e) {
                    // fall
                    logger.debug("Attempt to query for item failed, trying something else");
                }
            }

            BooleanQuery.Builder builder = new BooleanQuery.Builder();

            int nShouldMatch = 0;

            int iSlash = query.lastIndexOf('/');
            if (iSlash != -1) {
                builder = builder
                        .add(createEqualClassifierName(query.substring(0, iSlash)), BooleanClause.Occur.SHOULD);
                query = query.substring(iSlash+1);
                nShouldMatch++;
            }

            int iCardinal = query.lastIndexOf(':');
            if (iCardinal != -1) {
                String prediction = sanitize(query.substring(iCardinal+1));
                String head = sanitize(query.substring(0, iCardinal));
                if ("*".equals(prediction)) {
                    return createEqualCriterion(head);
                }
                nShouldMatch += 2;
                builder = builder
                        .add(createEqualPrediction(prediction), BooleanClause.Occur.SHOULD)
                        .add(createEqualCriterion(head), BooleanClause.Occur.SHOULD);

            } else {
                query = sanitize(query);
                if (!"*".equals(query)) {
                    nShouldMatch += 2;
                    builder = builder
                            .add(createEqualCriterion(query), BooleanClause.Occur.SHOULD)
                            .add(createEqualPrediction("1"), BooleanClause.Occur.SHOULD);
                }
            }
            return builder.setMinimumNumberShouldMatch(nShouldMatch).build();
        }

        private String sanitize(String token) {
            if (token.charAt(0) == '"' && token.charAt(token.length() - 1) == '"') {
                token = token.substring(1, token.length() - 1);
            }
            else if (token.charAt(0) == '(' && token.charAt(token.length() - 1) == ')') {
                token = token.substring(1, token.length() - 1).trim();
            }
            return token;
        }

        private Query createEqual(String termName, String termValue) {
            return new TermQuery(new Term(termName, termValue));
        }

        private Query createEqualItem(URI uri) {
            return createEqual(FIELD_URI, uri.toString());
        }

        protected Query createEqualClassifierName(String query) {
            return createEqual(FIELD_CLASSIFIER_NAME, query);
        }
        protected Query createEqualCriterion(String query) {
            return createEqual(FIELD_CRITERION, query);
        }
        protected Query createEqualPrediction(String query) {
            return createEqual(FIELD_PREDICTED_CLASS, query);
        }
    }
}
