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
package pt.ua.dicoogle.classdb.ws;

import javax.servlet.MultipartConfigElement;

import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.classdb.database.Database;
import pt.ua.dicoogle.sdk.JettyPluginInterface;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class WebServletPlugin implements JettyPluginInterface, PlatformCommunicatorInterface {
    private static final Logger logger = LoggerFactory.getLogger(WebServletPlugin.class);

    private boolean enabled;
    private ConfigurationHolder settings;
    private DicooglePlatformInterface platform;
    private Database db;
    private final ClassifyWebServlet wsClassify;
    private final QueryWebServlet wsQuery;

    public WebServletPlugin() {
        this.wsClassify = new ClassifyWebServlet();
        this.wsQuery = new QueryWebServlet();
        this.enabled = true;
    }

    public void setDatabase(Database db) {
        this.db = db;
        this.wsQuery.setDatabase(db);
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface pi) {
        this.platform = pi;
        // since web service is not a plugin interface, the platform interface must be provided manually
        this.wsClassify.setPlatformProxy(pi);
    }

    @Override
    public String getName() {
        return "class-db";
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
        return settings;
    }


    @Override
    public HandlerList getJettyHandlers() {

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/classification");

        ServletHolder classifyServletHolder = new ServletHolder(this.wsClassify);
        classifyServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement("/tmp/dicoogle"));
        handler.addServlet(classifyServletHolder, "/classify/*");

        ServletHolder queryServletHolder = new ServletHolder(this.wsQuery);
        handler.addServlet(queryServletHolder, "/query");

        HandlerList l = new HandlerList();
        l.addHandler(handler);

        return l;
    }
}
