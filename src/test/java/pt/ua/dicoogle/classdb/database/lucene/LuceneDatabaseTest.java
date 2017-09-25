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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pt.ua.dicoogle.classdb.database.Database;
import pt.ua.dicoogle.classdb.database.DatabaseReader;
import pt.ua.dicoogle.classdb.database.DatabaseWriter;
import pt.ua.dicoogle.classdb.database.struct.DatabaseItem;
import pt.ua.dicoogle.classdb.database.struct.DatabaseItemImpl;
import pt.ua.dicoogle.classdb.database.struct.QueryParameters;
import pt.ua.dicoogle.classdb.database.struct.QueryParametersBuilder;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class LuceneDatabaseTest {

    private Path dbPath;
    private Database database;

    @Before
    public void init() throws IOException {
        this.dbPath = Files.createTempDirectory("dicoogle-classdb");
        this.database = new LuceneDatabase(dbPath);
    }

    private static final List<DatabaseItem> TEST_LIST = Arrays.asList(
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "aorta", "true", 0.94),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "aorta", "false", 0.06),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "true", 0.85),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "false", 0.15),
            new DatabaseItemImpl("file://dataset/2.dcm", "convnet", "pancreas", "false", 0.8),
            new DatabaseItemImpl("file://dataset/2.dcm", "convnet", "pancreas", "true", 0.2),
            new DatabaseItemImpl("file://dataset/3.dcm", "convnet", "liver", "true", 0.11),
            new DatabaseItemImpl("file://dataset/3.dcm", "convnet", "liver", "false", 0.89),
            new DatabaseItemImpl("file://dataset/7.dcm", "bad", "liver", "true", 0.66),
            new DatabaseItemImpl("file://dataset/7.dcm", "bad", "liver", "false", 0.34)
    );

    private static final List<DatabaseItem> TEST_ALL_LIVER_LIST = Arrays.asList(
            new DatabaseItemImpl("file://dataset/3.dcm", "convnet", "liver", "false", 0.89),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "true", 0.85),
            new DatabaseItemImpl("file://dataset/7.dcm", "bad", "liver", "true", 0.66),
            new DatabaseItemImpl("file://dataset/7.dcm", "bad", "liver", "false", 0.34),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "false", 0.15),
            new DatabaseItemImpl("file://dataset/3.dcm", "convnet", "liver", "true", 0.11)
    );

    private static final List<DatabaseItem> TEST_001_LIST = Arrays.asList(
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "aorta", "true", 0.94),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "true", 0.85),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "false", 0.15),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "aorta", "false", 0.06)
    );

    private static final List<DatabaseItem> TEST_SPECIFIC_LIST = Arrays.asList(
            new DatabaseItemImpl("file://dataset/2.dcm", "convnet", "pancreas", "true", 0.2)
    );

    private static final List<DatabaseItem> TEST_GOOD_LIVER_LIST = Arrays.asList(
            new DatabaseItemImpl("file://dataset/3.dcm", "convnet", "liver", "false", 0.89),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "true", 0.85),
            new DatabaseItemImpl("file://dataset/7.dcm", "bad", "liver", "true", 0.66)
    );

    private static final List<DatabaseItem> TEST_POSITIVE_LIVER_LIST = Arrays.asList(
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "true", 0.85),
            new DatabaseItemImpl("file://dataset/7.dcm", "bad", "liver", "true", 0.66),
            new DatabaseItemImpl("file://dataset/3.dcm", "convnet", "liver", "true", 0.11)
    );

    private static final List<DatabaseItem> TEST_NEGATIVE_LIVER_LIST = Arrays.asList(
            new DatabaseItemImpl("file://dataset/3.dcm", "convnet", "liver", "false", 0.89),
            new DatabaseItemImpl("file://dataset/7.dcm", "bad", "liver", "false", 0.34),
            new DatabaseItemImpl("file://dataset/1.dcm", "a-classifier", "liver", "false", 0.15)
    );

    private void writeTestData() throws IOException {
        try (DatabaseWriter writer = database.createWriter()) {
            for (DatabaseItem item : TEST_LIST) {
                writer.add(item);
            }
        }

    }

    final List<DatabaseItem> TEST_OVERWITE_LIST = Arrays.asList(
            new DatabaseItemImpl("file://dataset/1/002.dcm", "mammo", "calcification", "true", 0.9),
            new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "calcification", "true", 0.85),
            new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "microcalcification", "true", 0.72),
            new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "microcalcification", "false", 0.28),
            new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "calcification", "false", 0.15),
            new DatabaseItemImpl("file://dataset/1/002.dcm", "mammo", "calcification", "false", 0.1)
    );

    @Test
    public void overwrite() throws IOException {
        try (DatabaseWriter writer = database.createWriter()) {
            writer.add(new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "calcification", "true", 0.54));
            writer.add(new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "calcification", "false", 0.46));
            writer.add(new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "calcification", "true", 0.85));
            writer.add(new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "calcification", "false", 0.15));
            writer.add(new DatabaseItemImpl("file://dataset/1/002.dcm", "mammo", "calcification", "true", 0.9));
            writer.add(new DatabaseItemImpl("file://dataset/1/002.dcm", "mammo", "calcification", "false", 0.1));
            writer.add(new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "microcalcification", "true", 0.72));
            writer.add(new DatabaseItemImpl("file://dataset/1/001.dcm", "mammo", "microcalcification", "false", 0.28));
        }

        DatabaseReader reader = database.createReader();
        Collection<DatabaseItem> allPreds = reader.search("mammo", new QueryParametersBuilder().setThreshold(0.f).build())
                .collect(Collectors.toList());
        assertEquals(TEST_OVERWITE_LIST, allPreds);

        Collection<DatabaseItem> allPreds2 = reader.search("classifier:mammo", new QueryParametersBuilder().build())
                .collect(Collectors.toList());
        assertEquals(TEST_OVERWITE_LIST, allPreds2);
    }

    @Test
    public void getSpecific() throws IOException {
        writeTestData();

        Collection<DatabaseItem> pancreasPred = database.search("convnet\\/pancreas:true", new QueryParametersBuilder()
                .build())
                .collect(Collectors.toList());

        assertEquals(TEST_SPECIFIC_LIST, pancreasPred);
    }

    @Test
    public void getAll() throws IOException {
        writeTestData();

        DatabaseReader reader = database.createReader();

        // perform the reading test twice (to make sure that a reader can be used multiple times)
        for (int i = 0 ; i < 2 ; i++) {
            Collection<DatabaseItem> liverPreds = reader.search("liver:(false OR true)", new QueryParameters())
                    .collect(Collectors.toList());

            assertEquals(TEST_ALL_LIVER_LIST, liverPreds);
        }
    }

    @Test
    public void getBestOnly() throws IOException {
        writeTestData();

        Collection<DatabaseItem> liverPreds = database.search("liver:(false OR true)", new QueryParametersBuilder()
                .setOnlyBest(true)
                .build())
                .collect(Collectors.toList());

        assertEquals(TEST_GOOD_LIVER_LIST, liverPreds);
    }

    @Test
    public void getGood() throws IOException {
        writeTestData();

        DatabaseReader reader = database.createReader();
        Collection<DatabaseItem> liverPreds = reader.search("liver:(false OR true)", new QueryParametersBuilder()
                    .setThreshold(0.5f)
                    .build())
                    .collect(Collectors.toList());

        assertEquals(TEST_GOOD_LIVER_LIST, liverPreds);

        Collection<DatabaseItem> liverPreds2 = reader.search("liver:false OR liver:true", new QueryParametersBuilder()
                .setThreshold(0.5f)
                .build())
                .collect(Collectors.toList());

        assertEquals(TEST_GOOD_LIVER_LIST, liverPreds2);
    }

    @Test
    public void getByUri() throws IOException {
        writeTestData();

        DatabaseReader reader = database.createReader();

        Collection<DatabaseItem> preds = reader.search("uri:\"file://dataset/1.dcm\"", new QueryParameters())
                .collect(Collectors.toList());

        assertEquals(TEST_001_LIST, preds);
    }

    @Test
    public void getPositive() throws IOException {
        writeTestData();

        DatabaseReader reader = database.createReader();
        Collection<DatabaseItem> liverPreds = reader.search("liver", new QueryParametersBuilder()
                .setThreshold(0.f)
                .build())
                .collect(Collectors.toList());

        assertEquals(TEST_POSITIVE_LIVER_LIST, liverPreds);
    }

    @Test
    public void getNegative() throws IOException {
        writeTestData();

        DatabaseReader reader = database.createReader();
        Collection<DatabaseItem> liverPreds = reader.search("liver:false", new QueryParametersBuilder()
                    .setThreshold(0.f)
                    .build())
                    .collect(Collectors.toList());

        assertEquals(TEST_NEGATIVE_LIVER_LIST, liverPreds);
    }

    @After
    public void cleanUp() throws IOException {
        database.close();
        Files.walkFileTree(dbPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw e;
                }
            }
        });
    }
}
