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
package pt.ua.dicoogle.classdb.database.struct;

import org.apache.commons.lang3.tuple.Pair;
import pt.ua.dicoogle.classification.api.PredictionIdentifier;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class DatabaseItemImpl implements DatabaseItem {
    private final URI uri;
    private final String classifierName;
    private final String criterion;
    private final String prediction;
    private final double score;

    public DatabaseItemImpl(URI uri, String classifierName, String criterion, String prediction, double score) {
        this.uri = uri;
        this.classifierName = classifierName;
        this.criterion = criterion;
        this.prediction = prediction;
        this.score = score;
    }

    public DatabaseItemImpl(String uri, String classifierName, String criterion, String prediction, double score) {
        this(URI.create(uri), classifierName, criterion, prediction, score);
    }

    @Override
    public URI item() {
        return uri;
    }

    @Override
    public String classifierName() {
        return classifierName;
    }

    @Override
    public String criterion() {
        return criterion;
    }

    @Override
    public String predictedClass() {
        return prediction;
    }

    @Override
    public double score() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseItemImpl that = (DatabaseItemImpl) o;
        return Double.compare(that.score, score) == 0 &&
                Objects.equals(uri, that.uri) &&
                Objects.equals(classifierName, that.classifierName) &&
                Objects.equals(criterion, that.criterion) &&
                Objects.equals(prediction, that.prediction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, classifierName, criterion, prediction, score);
    }

    @Override
    public String toString() {
        return "DatabaseItemImpl{" +
                "uri=" + uri +
                ", classifierName='" + classifierName + '\'' +
                ", criterion='" + criterion + '\'' +
                ", prediction='" + prediction + '\'' +
                ", score=" + score +
                '}';
    }
}
