/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.websearch;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import com.frostwire.android.util.IOUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class HttpClient {

    private static final String DEFAULT_USER_AGENT = "Wget/1.12";
    private static final int DEFAULT_TIMEOUT = 5000;

    private final String url;
    private final String userAgent;
    private final int timeout;

    public HttpClient(String url) {
        this.url = url;
        this.userAgent = DEFAULT_USER_AGENT;
        this.timeout = DEFAULT_TIMEOUT;
    }

    public String get() {
        String body = null;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            setupConnection(conn);

            try {
                InputStream in = new BufferedInputStream(conn.getInputStream());
                body = IOUtils.toString(in, Charset.forName("UTF-8"));
            } finally {
                conn.disconnect();
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return body;
    }

    private void setupConnection(HttpURLConnection conn) {
        conn.setInstanceFollowRedirects(true); // not necessary since it's true by default.
        conn.setRequestProperty("User-Agent", userAgent);

        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
    }
}
