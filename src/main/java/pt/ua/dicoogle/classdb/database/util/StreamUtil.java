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

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class StreamUtil {
    private StreamUtil() {}

    /** Aggregate the given elements from a stream into a list of classes according
     * to the given criterion. The aggregation key of each element is retrieved
     * using the given function. Elements of which a comparison with "equals()"
     * returns true will be kept in the same list.
     * This aggregation preserves the order of occurrence of both the base list
     * and class lists.
     *
     * @param <T> the element type
     * @param <K> the type of aggregation key
     * @param elements a stream containing the elements to aggregate. The stream will be fully consumed.
     * @param criterion a function that establishes an aggregation key for each element.
     * @return a list L of lists l(i), where each l contains elements of equal class.
     */
    public static <K extends Comparable<K>, T> List<List<T>> orderedGroupBy(Stream<T> elements, Function<? super T, ? extends K> criterion) {
        Map<K, List<T>> bag = new TreeMap<>();
        List<List<T>> aggregation = new ArrayList<>();
        elements.forEachOrdered(res -> {
            K key = criterion.apply(res);
            List<T> thisList = bag.get(key);
            if (thisList == null) {
                thisList = new ArrayList<>();
                bag.put(key, thisList);
                aggregation.add(thisList);
            }
            thisList.add(res);
        });

        return aggregation;
    }

    /** Obtain a stream of only the first entry to occur in the stream with the same aggregation key.
     * This method will eagerly consume the given stream and produce a new one.
     *
     * @param stream the stream to consume
     * @param keyFn a function for retrieving the aggregation key of each element.
     * @param <K> the aggregation key type
     * @param <T> the base stream element type
     * @return a new stream of T
     */
    public static <K extends Comparable<K>, T> Stream<T> firstOf(Stream<T> stream, Function<? super T, ? extends K> keyFn) {
        return orderedGroupBy(stream, keyFn).stream()
                .map(l -> l.iterator().next());
    }

    /** Obtain a stream of only the best entries (the first element under their natural order) of their aggregating
     * key. This method will eagerly consume the given stream and produce a new one.
     *
     * @param stream the stream to consume
     * @param keyFn a function for retrieving the aggregation key of each element.
     * @param <K> the aggregation key type
     * @param <T> the base stream element type
     * @return a new stream of T
     */
    public static <K extends Comparable<K>, T extends Comparable<T>> Stream<T> bestOf(Stream<T> stream, Function<? super T,? extends K> keyFn) {
        return orderedGroupBy(stream, keyFn).stream()
                .map(Collections::min);
    }

    /** Obtain a stream of only the best entries (the first element according to the given comparator of their
     * aggregating key. This method will eagerly consume the given stream and produce a new one.
     *
     * @param stream the stream to consume
     * @param keyFn a function for retrieving the aggregation key of each element.
     * @param <K> the aggregation key type
     * @param <T> the base stream element type
     * @return a new stream of T
     */
    public static <K extends Comparable<K>, T> Stream<T> bestOf(Stream<T> stream, Function<? super T, ? extends K> keyFn, Comparator<? super T> comparator) {
        return orderedGroupBy(stream, keyFn).stream()
                .map(e -> Collections.min(e, comparator));
    }
}
