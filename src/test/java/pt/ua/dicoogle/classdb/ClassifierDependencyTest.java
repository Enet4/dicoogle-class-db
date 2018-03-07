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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class ClassifierDependencyTest {

    private final ClassificationEndpointDescriptor desc1 = new ClassificationEndpointDescriptor("x", "1",
            Arrays.asList());
    private final ClassificationEndpointDescriptor desc2 = new ClassificationEndpointDescriptor("x", "2",
            Arrays.asList("1"));
    private final ClassificationEndpointDescriptor desc3 = new ClassificationEndpointDescriptor("x", "3",
            Arrays.asList("1", "2"));
    private final ClassificationEndpointDescriptor desc4 = new ClassificationEndpointDescriptor("x", "4",
            Arrays.asList("3"));
    private final ClassificationEndpointDescriptor desc5 = new ClassificationEndpointDescriptor("x", "5",
            Arrays.asList("6"));
    private final ClassificationEndpointDescriptor desc6 = new ClassificationEndpointDescriptor("x", "6",
            Arrays.asList("5"));

    @Test
    public void test1() {
        final List<ClassificationEndpointDescriptor> list = Arrays.asList(
                desc1, desc2
        );
        final List<ClassificationEndpointDescriptor> expected = Arrays.asList(
                desc1, desc2
        );

        assertEquals(expected,
                ClassificationIndexer.sortByDependencies(new ArrayList<>(list)));
    }

    @Test
    public void test2() {
        final List<ClassificationEndpointDescriptor> list = Arrays.asList(
                desc2, desc1
        );
        final List<ClassificationEndpointDescriptor> expected = Arrays.asList(
                desc1, desc2
        );

        assertEquals(expected,
                ClassificationIndexer.sortByDependencies(new ArrayList<>(list)));
    }

    @Test
    public void test3() {
        final List<ClassificationEndpointDescriptor> list = Arrays.asList(
                desc4, desc2, desc3, desc1
        );
        final List<ClassificationEndpointDescriptor> expected = Arrays.asList(
                desc1, desc2, desc3, desc4
        );

        assertEquals(expected,
                ClassificationIndexer.sortByDependencies(new ArrayList<>(list)));
    }

    @Test
    public void test4() {
        final List<ClassificationEndpointDescriptor> list = Arrays.asList(
                desc3, desc1, desc4
        );
        final List<ClassificationEndpointDescriptor> expected = Arrays.asList(
                desc1, desc3, desc4
        );

        assertEquals(expected,
                ClassificationIndexer.sortByDependencies(new ArrayList<>(list)));
    }

    @Test
    public void test5() {
        final List<ClassificationEndpointDescriptor> list = Arrays.asList(
                desc5, desc6
        );
        Object out = ClassificationIndexer.sortByDependencies(new ArrayList<>(list));
        assertNull("Cyclic dependency!", out);
    }

    @Test
    public void test6() {
        final List<ClassificationEndpointDescriptor> list = Arrays.asList(
                desc5, desc1, desc6
        );
        Object out = ClassificationIndexer.sortByDependencies(new ArrayList<>(list));
        assertNull("Cyclic dependency!", out);
    }
}
