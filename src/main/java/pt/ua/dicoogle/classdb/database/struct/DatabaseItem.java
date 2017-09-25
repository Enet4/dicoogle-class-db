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

import pt.ua.dicoogle.classification.api.PredictionIdentifier;

import java.net.URI;

/** An interface for classification database items.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public interface DatabaseItem {

    /** Obtain the URI of the classified item.
     *
     * @return a URI referring to an item in Dicoogle storage
     */
    public URI item();

    /** Obtain the name of the classifier that resulted in this prediction.
     *
     * @return the classifier's unique name
     */
    public String classifierName();

    /** Obtain the classification criterion (the class family).
     *
     * @return the classification criterion
     */
    public String criterion();

    /** Obtain the name of the predicted class.
     *
     * @return the name of the predicted class
     */
    public String predictedClass();

    /** Obtain a prediction identifier from this database item.
     * The default implementation simply combines the classifier name, the criterion and
     * the predicted class.
     *
     * @return an item identifying the prediction associated to this item
     */
    public default PredictionIdentifier predictionIdentifier() {
        return new PredictionIdentifier(classifierName(), criterion(), predictedClass());
    }

    /** Obtain the probability value of the classification's success.
     *
     * @return the score, a real number between 0 and 1.
     */
    public double score();

    public static DatabaseItem of(URI uri, PredictionIdentifier predictionId, double score) {
        return new DatabaseItem() {
            @Override
            public URI item() {
                return uri;
            }

            @Override
            public String classifierName() {
                return predictionId.getClassifierName();
            }

            @Override
            public String criterion() {
                return predictionId.getCriterion();
            }

            @Override
            public String predictedClass() {
                return predictionId.getPredictionClass();
            }

            @Override
            public PredictionIdentifier predictionIdentifier() {
                return predictionId;
            }

            @Override
            public double score() {
                return score;
            }
        };
    }
}
