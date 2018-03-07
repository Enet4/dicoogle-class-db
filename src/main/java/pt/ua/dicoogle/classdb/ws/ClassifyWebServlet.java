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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class ClassifyWebServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ClassifyWebServlet.class);

    private DicooglePlatformInterface platform;

    public void setPlatformProxy(DicooglePlatformInterface platform) {
        this.platform = platform;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String requestURI = req.getRequestURI().substring("/classification/classify/".length());
            String[] resArr = requestURI.split("/");

            String classifierName;
            String criterion;
            try {
                classifierName = resArr[0];
                criterion = resArr[1];
            } catch (ArrayIndexOutOfBoundsException ex) {
                sendError(resp, 400, "Bad path to classifier/criterion");
                return;
            }
            String data = req.getParameter("uri");
            if (data == null) {
                sendError(resp, 400, "Missing uri parameter");
                return;
            }

            // use query provider as a classifier
            QueryInterface classifier = platform.getQueryProviderByName(classifierName, true);
            if (classifier == null) {
                sendError(resp, 400, "No such classifier with the name " + classifierName);
                return;
            }

            long ctime = System.currentTimeMillis();

            Map<String, Object> out = StreamSupport.stream(classifier.query(criterion, data).spliterator(), false)
                    .filter(sr -> sr.getURI().getScheme().equalsIgnoreCase("class"))
                    .collect(HashMap<String,Object>::new, (m, searchResult) -> {
                        URI uri = searchResult.getURI();
                        double score = searchResult.getScore();
                        if (Double.isFinite(score)) {
                            m.put(uri.toString(), searchResult.getScore());
                        } else {
                            m.put(uri.toString(), Double.toString(score));
                        }
                    }, Map::putAll);
            long etime = System.currentTimeMillis() - ctime;
            JSONObject o = new JSONObject();
            o.put("results", new JSONObject(out));
            o.put("elapsedTime", etime);
            resp.setStatus(200);
            resp.getWriter().write(o.toString());

        } catch (RuntimeException|JSONException ex) {
            logger.warn("Servlet failure", ex);
            sendError(resp, 500, "Internal server failure");
        }
    }

    private static void sendError(HttpServletResponse resp, int code, String message) throws IOException {
        JSONObject o = new JSONObject();
        try {
            o.put("error", message);
        } catch (JSONException e) { throw new IOException(e); }
        resp.setStatus(code);
        resp.getWriter().write(o.toString());
    }
}
