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
package pt.ua.dicoogle.classdb.database;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import pt.ua.dicoogle.classdb.database.struct.DatabaseItem;
import pt.ua.dicoogle.classdb.database.struct.QueryParameters;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public interface Database extends DatabaseWriter, DatabaseReader, Closeable {
    
    public DatabaseWriter createWriter() throws IOException;

    public DatabaseReader createReader() throws IOException;

    public default DatabaseWriter add(DatabaseItem item) throws IOException {
        try (DatabaseWriter writer = this.createWriter()) {
            writer.add(item);
        }
        return this;
    }

    public default Stream<DatabaseItem> search(String query, QueryParameters params) throws IOException {
        return this.createReader().search(query, params);
    }

    public default Stream<DatabaseItem> search(String query) throws IOException {
        return this.createReader().search(query);
    }

    public default boolean remove(URI item) throws IOException {
        try (DatabaseWriter writer = this.createWriter()) {
            return writer.remove(item);
        }
    }
}
