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

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.classdb.database.Database;
import pt.ua.dicoogle.classdb.database.lucene.LuceneDatabase;
import pt.ua.dicoogle.classdb.ws.WebServletPlugin;
import pt.ua.dicoogle.sdk.*;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

/**
 * The main plugin set.
 *
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
@PluginImplementation
public class ClassificationDatabaseSet implements PluginSet {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationDatabaseSet.class);

    private final ClassificationIndexer indexer;
    private final QueryProvider query;
    private final WebServletPlugin ws;
    private Database db;

    private ConfigurationHolder settings;

    public ClassificationDatabaseSet() {
        logger.debug("Initializing Classification Database Plugin Set");

        // construct all plugins here
        this.indexer = new ClassificationIndexer();
        this.query = new QueryProvider();
        this.ws = new WebServletPlugin();
        logger.info("Classification Database is ready");
    }

    private void initDatabase(Path dir) throws IOException {
        this.db = new LuceneDatabase(dir);
        this.query.setDatabase(this.db);
        this.indexer.setDatabase(this.db);
        this.ws.setDatabase(this.db);
    }

    @Override
    public Collection<IndexerInterface> getIndexPlugins() {
        return Collections.singleton(this.indexer);
    }

    @Override
    public Collection<QueryInterface> getQueryPlugins() {
        return Collections.singleton(this.query);
    }

    /**
     * This method is used to retrieve a name for identifying the plugin set.
     *
     * @return a unique name for the plugin set
     */
    @Override
    public String getName() {
        return "class-db";
    }

    @Override
    public Collection<ServerResource> getRestPlugins() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<JettyPluginInterface> getJettyPlugins() {
        return Collections.singleton(ws);
    }

    @Override
    public void shutdown() {
        try {
            this.db.close();
        } catch (IOException e) {
            logger.warn("Failed to close class-db index", e);
        }
    }

    @Override
    public Collection<StorageInterface> getStoragePlugins() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;

        HierarchicalConfiguration conf = xmlSettings.getConfiguration();
        conf.setThrowExceptionOnMissing(true);
        String indexPath;
        try {
            indexPath = conf.getString("index-path");
        } catch (NoSuchElementException e) {
            indexPath = "class-db";
        }
        if (this.db == null) {
            try {
                this.initDatabase(new File(indexPath).toPath());
            } catch (IOException e) {
                logger.warn("Failed to initialize classification database", e);
            }
        }
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    @Override
    public Collection<GraphicalInterface> getGraphicalPlugins() {
        return Collections.EMPTY_LIST;
    }
}
