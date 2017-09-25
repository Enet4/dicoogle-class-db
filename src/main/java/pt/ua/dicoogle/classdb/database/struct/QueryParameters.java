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

/** Immutable object type for holding classification database query parameters.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class QueryParameters {
    public static final QueryParameters DEFAULT = new QueryParameters();

    private final int nresults;
    private final float threshold;
    private final boolean onlybest;

    /** Create a new set of query parameters.
     *
     * @param nresults the number of results to retrieve
     * @param threshold the minimum (exclusive) probability value in order to accept a prediction
     * @param onlybest whether to retrieve only the best class prediction of each item-criterion pair.
     */
    public QueryParameters(int nresults, float threshold, boolean onlybest) {
        if (nresults < -1) {
            throw new IllegalArgumentException();
        }
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException();
        }
        this.nresults = nresults;
        this.threshold = threshold;
        this.onlybest = onlybest;
    }

    public QueryParameters() {
        this(-1, 0.f, false);
    }

    /**
     * @return the number of results to retrieve, -1 for all
     */
    public int getNumberOfResults() {
        return this.nresults;
    }

    public float getThreshold() {
        return threshold;
    }

    public boolean isOnlyBest() {
        return onlybest;
    }
}
