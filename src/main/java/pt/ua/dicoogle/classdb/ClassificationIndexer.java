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
package pt.ua.dicoogle.classdb;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.classdb.database.Database;
import pt.ua.dicoogle.classdb.database.DatabaseWriter;
import pt.ua.dicoogle.classdb.database.struct.DatabaseItem;
import pt.ua.dicoogle.classdb.database.util.LazyDictionary;
import pt.ua.dicoogle.classification.api.ClassifierDescriptor;
import pt.ua.dicoogle.classification.api.PredictionIdentifier;
import pt.ua.dicoogle.sdk.IndexerInterface;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.datastructs.IndexReport;
import pt.ua.dicoogle.sdk.datastructs.IndexReport2;
import pt.ua.dicoogle.sdk.datastructs.Report;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.dicoogle.sdk.task.ProgressCallable;
import pt.ua.dicoogle.sdk.task.Task;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class ClassificationIndexer implements IndexerInterface, PlatformCommunicatorInterface {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationIndexer.class);

    private static DicooglePlatformInterface platform = null;
    private boolean enabled = true;
    private boolean estimateProgress = false;
    private ConfigurationHolder settings = null;
    private Database db = null;
    private ClassifierDescriptor dec;
    private List<ClassificationEndpointDescriptor> classifierEndpoints = null;
    private LazyDictionary<String, QueryInterface> classifiers = new LazyDictionary<>(name -> {
        if (platform == null) {
            throw new IllegalStateException("Dicoogle platform not ready!");
        }
        return platform.getQueryProviderByName(name, true);
    });

    public void setDatabase(Database db) {
        this.db = db;
    }

    private IndexReport classifyAndIndex(StorageInputStream storage, DatabaseWriter writer) throws IOException {
        final URI uri = storage.getURI();
        logger.info("Classifying and indexing {} ...", uri);

        return this.classifierEndpoints.stream().sequential()
                // flatten all predictions
                .flatMap(p -> {
                    final QueryInterface qint = this.classifiers.get(p.getClassifierName());
                    if (qint == null) {
                        logger.warn("No such classifier {}, providing no predictions");
                        return Stream.empty();
                    }
                    return ((Collection<SearchResult>) qint.query(p.getCriterion(), uri)).stream();
                })
                // ignore invalid output
                .filter(sr -> sr.getScore() >= 0 && sr.getScore() <= 1)
                .<IndexReport>map(res -> {
                    final DatabaseItem dbItem = this.fromSearchResult(uri, res);
                    try {
                        writer.add(dbItem);
                        return new IndexReport2(1, 0);
                    } catch (IOException|RuntimeException e) {
                        logger.warn("Could not add item {}", dbItem, e);
                        return new IndexReport2(0, 1);
                    }
                }).reduce(new IndexReport2(0,0), ClassificationIndexer::merged);
    }

    private static IndexReport merged(IndexReport r1, IndexReport r2) {
        return new IndexReport2(r1.getNIndexed() + r2.getNIndexed(), r1.getNErrors() + r2.getNErrors());
    }

    @Override
    public Task<Report> index(final StorageInputStream file, Object... args) {

        return new Task<>(
                new ProgressCallable<Report>() {
            private float progress = 0.0f;

            @Override
            public Report call() throws Exception {
                if (ClassificationIndexer.this.classifierEndpoints == null || platform == null) {
                    logger.warn("Indexer is not ready!");
                    return new Report();
                }
                Objects.requireNonNull(ClassificationIndexer.this.db);
                final long currTime = System.currentTimeMillis();

                IndexReport r;
                try (DatabaseWriter writer = ClassificationIndexer.this.db.createWriter()) {
                    r = ClassificationIndexer.this.classifyAndIndex(file, writer);
                } catch (Exception e) {
                    logger.warn("Failed to index {}", file.getURI(), e);
                    r = new IndexReport2(0, 1);
                }

                progress = 1.0f;

                return new IndexReport2(r.getNIndexed(), r.getNErrors(), System.currentTimeMillis() - currTime);
            }

            @Override
            public float getProgress() {
                return progress;
            }
        });

    }

    @Override
    public Task<Report> index(final Iterable<StorageInputStream> files, Object... args) {
        return new Task<>(new ProgressCallable<Report>() {
            private float progress = 0.0f;

            @Override
            public Report call() throws Exception {
                if (ClassificationIndexer.this.classifierEndpoints == null || platform == null) {
                    logger.warn("Indexer is not ready!");
                    return new Report();
                }
                Objects.requireNonNull(ClassificationIndexer.this.db);
                final long currTime = System.currentTimeMillis();

                Iterable<StorageInputStream> allFiles;
                float part = 0;

                if (ClassificationIndexer.this.estimateProgress) {
                    allFiles = StreamSupport.stream(files.spliterator(), false)
                            .collect(Collectors.toList());
                    part = 1.f / ((List)allFiles).size();
                } else {
                    allFiles = files;
                    this.progress = -1;
                }
                IndexReport r = new IndexReport2();
                try (DatabaseWriter writer = ClassificationIndexer.this.db.createWriter()) {
                    for (StorageInputStream f : allFiles) {
                        try {
                            r = merged(r, ClassificationIndexer.this.classifyAndIndex(f, writer));
                            if (ClassificationIndexer.this.estimateProgress) {
                                progress += part;
                            }
                        } catch (RuntimeException ex) {
                            logger.warn("Failed to index {}", f.getURI(), ex);
                        }
                    }
                }

                progress = 1.0f;

                return new IndexReport2(r.getNIndexed(), r.getNErrors(), System.currentTimeMillis() - currTime);
            }

            @Override
            public float getProgress() {
                return progress;
            }
        });

    }

    @Override
    public boolean unindex(URI uri) {
        if (ClassificationIndexer.this.db == null) {
            logger.warn("Indexer is not ready!");
            return false;
        }
        try {
            return this.db.remove(uri);
        } catch (IOException e) {
            logger.warn("Failed to unindex \"{}\"", e);
            return false;
        }
    }

    /**
     * This method is used to retrieve the unique name of the indexer.
     *
     * @return a fixed name for the indexer
     */
    @Override
    public String getName() {
        return "class-db";
    }

    @Override
    public boolean enable() {
        this.enabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.enabled = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setSettings(ConfigurationHolder settings) {
        this.settings = settings;
        // use settings here
        XMLConfiguration conf = settings.getConfiguration();
        conf.setThrowExceptionOnMissing(true);
        this.classifierEndpoints = new ArrayList<>();

        try {
            this.estimateProgress = conf.getBoolean("estimate-progress");
        } catch (RuntimeException ex) {
            conf.setProperty("estimate-progress", false);
        }

        try {
            conf.configurationAt("classifiers");
        } catch (RuntimeException ex) {
            conf.setProperty("classifiers", Collections.EMPTY_LIST);
        }

        Collection<HierarchicalConfiguration> cClassifiers = conf.configurationsAt("classifiers(0).classifier");

        List<ClassificationEndpointDescriptor> descriptors = cClassifiers.stream()
                .flatMap(elem -> {
                    String classifierName = elem.getString("[@name]");
                    logger.debug("registering classifier {} for indexation", classifierName);
                    return elem.configurationsAt("criterion").stream()
                            .map(c -> {
                                String criterion = c.getString("[@id]");
                                boolean binary = c.getBoolean("[@binary]", false);
                                List<String> depends = c.getList("[@depends]", Collections.emptyList()).stream()
                                        .map(o -> o.toString())
                                        .collect(Collectors.toList());
                                return new ClassificationEndpointDescriptor(classifierName, criterion, depends, binary);
                            });
                    }
                ).collect(Collectors.toList());

        // sort endpoints by dependencies
        this.classifierEndpoints = sortByDependencies(descriptors);
        logger.debug("Classification order: {},", this.classifierEndpoints);
        if (this.classifierEndpoints == null) {
            logger.warn("Cyclic dependency detected! Please check your configuration file.");
            logger.warn("Classification indexing disabled due to the previous error.");
            this.classifierEndpoints = Collections.EMPTY_LIST;
        }

        try {
            conf.save();
        } catch (ConfigurationException e) {
            logger.warn("Failed to save configurations", e);
        }
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    @Override
    public boolean handles(URI path) {
        // State here whether this indexer can index the file at the given path.
        // If not sure, simply return true and let the indexing procedures find out.
        return true;
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface dicooglePlatformInterface) {
        platform = dicooglePlatformInterface;
    }

    protected DatabaseItem fromSearchResult(URI uri, SearchResult result) {
        assert uri != null;
        assert result != null;
        PredictionIdentifier pred = PredictionIdentifier.decompose(result.getURI());
        return DatabaseItem.of(uri, pred, result.getScore());
    }

    public static List<ClassificationEndpointDescriptor> sortByDependencies(List<ClassificationEndpointDescriptor> descriptors) {
        boolean sorted;
        sorted = true;
        for (int i = 0; i < descriptors.size() - 1; i++) {
            for (int j = i+1; j < descriptors.size(); j++) {
                ClassificationEndpointDescriptor d1 = descriptors.get(i);
                ClassificationEndpointDescriptor d2 = descriptors.get(j);
                if (d1.dependsOn(d2.getCriterion())) {
                    descriptors.set(i, d2);
                    descriptors.set(j, d1);
                }
            }
        }
        // perform a second pass to identify cyclic dependencies
        for (int i = 0; i < descriptors.size() - 1; i++) {
            for (int j = i+1; j < descriptors.size(); j++) {
                ClassificationEndpointDescriptor d1 = descriptors.get(i);
                ClassificationEndpointDescriptor d2 = descriptors.get(j);
                if (d1.dependsOn(d2.getCriterion())) {
                    sorted = false;
                    break;
                }
            }
        }
        if (!sorted) {
            // cyclic dependency detected!
            return null;
        }
        return descriptors;
    }
}
