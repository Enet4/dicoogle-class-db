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
package pt.ua.dicoogle.classdb.database.util;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class RuntimeIOException extends RuntimeException {

    public RuntimeIOException() {
    }

    public RuntimeIOException(String s) {
        super(s);
    }

    public RuntimeIOException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public RuntimeIOException(Throwable throwable) {
        super(throwable);
    }
}
