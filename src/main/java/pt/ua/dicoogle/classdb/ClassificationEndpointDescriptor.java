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

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class ClassificationEndpointDescriptor {

    private final String classifierName;
    private final String criterion;
    private final Collection<String> depends;
    private final boolean binary;

    public ClassificationEndpointDescriptor(String classifierName, String criterion, Collection<String> dependencies) {
        this(classifierName, criterion, dependencies, false);
    }

    public ClassificationEndpointDescriptor(String classifierName, String criterion, Collection<String> dependencies, boolean binary) {
        Objects.requireNonNull(classifierName);
        Objects.requireNonNull(criterion);
        this.classifierName = classifierName;
        this.criterion = criterion;
        this.depends = dependencies;
        this.binary = binary;
    }

    public String getClassifierName() {
        return classifierName;
    }

    public String getCriterion() {
        return criterion;
    }

    public boolean isBinary() {
        return binary;
    }

    public Collection<String> getDependencies() {
        return this.depends;
    }

    public boolean dependsOn(String criterion) {
        return this.depends.contains(criterion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassificationEndpointDescriptor that = (ClassificationEndpointDescriptor) o;
        return binary == that.binary &&
                Objects.equals(classifierName, that.classifierName) &&
                Objects.equals(criterion, that.criterion) &&
                Objects.equals(depends, that.depends);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classifierName, criterion, depends, binary);
    }

    @Override
    public String toString() {
        return "ClassificationEndpointDescriptor{" +
                "classifierName='" + classifierName + '\'' +
                ", criterion='" + criterion + '\'' +
                ", depends=" + depends +
                ", binary=" + binary +
                '}';
    }
}
