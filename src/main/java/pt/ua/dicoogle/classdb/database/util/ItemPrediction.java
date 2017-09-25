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

import pt.ua.dicoogle.classdb.database.struct.DatabaseItem;

import java.net.URI;
import java.util.Objects;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class ItemPrediction implements Comparable<ItemPrediction> {

    public final URI item;
    public final String criterion;

    public ItemPrediction(URI item, String criterion) {
        this.item = item;
        this.criterion = criterion;
    }

    public static ItemPrediction from(DatabaseItem dbItem) {
        return new ItemPrediction(dbItem.item(), dbItem.criterion());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemPrediction that = (ItemPrediction) o;
        return Objects.equals(item, that.item) &&
                Objects.equals(criterion, that.criterion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, criterion);
    }

    @Override
    public String toString() {
        return "ItemPrediction{" +
                "item=" + item +
                ", criterion='" + criterion + '\'' +
                '}';
    }

    @Override
    public int compareTo(ItemPrediction itemPrediction) {
        int c1 = this.criterion.compareTo(itemPrediction.criterion);
        if (c1 != 0) return c1;
        return this.item.compareTo(itemPrediction.item);
    }
}
