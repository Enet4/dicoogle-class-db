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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.classdb.database.Database;
import pt.ua.dicoogle.classdb.database.struct.DatabaseItem;
import pt.ua.dicoogle.classdb.database.struct.QueryParametersBuilder;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class QueryProvider implements QueryInterface {

    private static final Logger logger = LoggerFactory.getLogger(QueryProvider.class);

    public static final String NAME = "class-db";

    private boolean enabled;
    private ConfigurationHolder settings;
    private Database db;

    public QueryProvider() {
        this.db = null;
        this.enabled = true;
    }

    public void setDatabase(Database db) {
        this.db = db;
    }

    @Override
    public Iterable<SearchResult> query(String query, Object... parameters) {
        if (this.db == null) {
            logger.warn("Illegal state: classification database is not ready!");
            return Collections.EMPTY_LIST;
        }
        logger.debug("Classification database query for: {}", query);
        try {
            QueryParametersBuilder qp = new QueryParametersBuilder();
            qp.setOnlyBest(true);
            if (parameters.length > 0 && parameters[0] instanceof Map) {
                Map<String, Object> paramDict = (Map<String, Object>)parameters[0];
                if (paramDict.containsKey("threshold")) {
                    qp.setThreshold((float)paramDict.get("threshold"));
                }
                if (paramDict.containsKey("nresults")) {
                    qp.setNresults((int)paramDict.get("nresults"));
                }
                if (paramDict.containsKey("onlybest")) {
                    qp.setOnlyBest((boolean)paramDict.get("onlybest"));
                }
            }
            Spliterator<SearchResult> splt = db.search(query, qp.build())
                        .map(item -> toSearchResult(item))
                        .spliterator();
            return new Iterable<SearchResult>() {
                @Override
                public Iterator<SearchResult> iterator() {
                    return Spliterators.iterator(splt);
                }

                @Override
                public Spliterator<SearchResult> spliterator() {
                    return splt;
                }
            };
        } catch (IOException|RuntimeException ex) {
            logger.warn("class-db query failed", ex);
            return Collections.EMPTY_LIST;
        }
    }

    protected SearchResult toSearchResult(DatabaseItem dbItem) {
        assert dbItem != null;
        HashMap<String, Object> extra = new HashMap<>();
        extra.put("id", dbItem.predictionIdentifier().toURI());
        return new SearchResult(dbItem.item(), dbItem.score(), extra);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean enable() {
        this.enabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.enabled = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setSettings(ConfigurationHolder settings) {
        this.settings = settings;
        // use settings here
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

}
