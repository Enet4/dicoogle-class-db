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

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.classdb.database.Database;
import pt.ua.dicoogle.classdb.database.struct.DatabaseItem;
import pt.ua.dicoogle.classdb.database.struct.QueryParametersBuilder;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;

/** A web servlet providing a service similar to Dicoogle search, tailored for this classification database.
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class QueryWebServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(QueryWebServlet.class);

    private volatile DicooglePlatformInterface platform;
    private volatile Database db;

    public void setPlatformProxy(DicooglePlatformInterface platform) {
        this.platform = platform;
    }

    public void setDatabase(Database db) {
        this.db = db;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String query = req.getParameter("query");
            if (query == null) {
                this.sendError(resp, 400, "Missing query parameter");
                return;
            }

            QueryParametersBuilder qp = new QueryParametersBuilder();
            String qOnlyBest = req.getParameter("onlybest");
            if (qOnlyBest != null) {
                qp.setOnlyBest(Boolean.parseBoolean(qOnlyBest));
            }
            String qThreshold = req.getParameter("threshold");
            if (qThreshold != null) {
                try {
                    float t = Float.parseFloat(qThreshold);
                    if (t < 0.f || t > 1.f) {
                        throw new NumberFormatException();
                    }
                    qp.setThreshold(t);
                } catch (NumberFormatException ex) {
                    this.sendError(resp, 400, "Bad threshold parameter: must be a number between 0 and 1");
                    return;
                }
            }
            String qNResults = req.getParameter("nresults");
            if (qNResults != null) {
                try {
                    int n = Integer.parseInt(qThreshold);
                    if (n < -1) {
                        n = -1;
                    }
                    qp.setNresults(Integer.parseInt(qNResults));
                } catch (NumberFormatException ex) {
                    this.sendError(resp, 400, "Bad nresults parameter: must be an integer");
                    return;
                }
            }

            // use database directly
            if (this.db == null) {
                this.sendError(resp, 500, "Classification database is not ready");
                return;
            }

            long ctime = System.currentTimeMillis();

            Stream<DatabaseItem> resultStream = this.db.search(query, qp.build());

            JSONObject o = new JSONObject();
            o.put("results", resultStream
                    .map(item -> {
                        JSONObject p = new JSONObject();
                        try {
                            p.put("item", item.item());
                            p.put("classifierName", item.classifierName());
                            p.put("criterion", item.criterion());
                            p.put("prediction", item.predictedClass());
                            p.put("score", item.score());
                        } catch (JSONException e) {
                            logger.warn("JSON problem", e);
                        }
                        return p;
                    }).collect(Collectors.toList())
            );
            long etime = System.currentTimeMillis() - ctime;
            o.put("elapsedTime", etime);
            resp.setStatus(200);
            resp.getWriter().write(o.toString());

        } catch (RuntimeException|JSONException ex) {
            logger.warn("Servlet failure", ex);
            this.sendError(resp, 500, "Internal server failure");
        }
    }

    private static void sendError(HttpServletResponse resp, int code, String message) throws IOException {
        JSONObject o = new JSONObject();
        try {
            o.put("error", message);
        } catch (JSONException e) {}
        resp.setStatus(code);
        resp.getWriter().write(o.toString());
    }
}
