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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class LazyText {

    private LazyText() {}

    public static LazyText of(final Supplier<?> fn) {
        Objects.requireNonNull(fn);

        return new LazyText() {

            public String toString() {
                return fn.get().toString();
            }

        };
    }
}
