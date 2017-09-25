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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class LazyDictionary<K, V> {
    private final Map<K, V> map;
    private final Function<K, V> resolver;

    public LazyDictionary(Function<K, V> resolver) {
        this.map = new HashMap<>();
        this.resolver = resolver;
    }

    public V get(K key) {
        if (!this.map.containsKey(key)) {
            return this.map.put(key, this.resolver.apply(key));
        }
        return this.map.get(key);
    }

    public void clear() {
        this.map.clear();
    }

    public Stream<V> values(Stream<K> keys) {
        return keys.map(k -> this.get(k));
    }

    public Stream<V> values(Collection<K> keys) {
        return this.values(keys.stream());
    }
}
