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

package com.frostwire.websearch.cookie;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
    private final Map<String, HttpCookie> cookies;

    static {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
    }

    public HttpClient(String url) {
        this.url = url;
        this.userAgent = DEFAULT_USER_AGENT;
        this.timeout = DEFAULT_TIMEOUT;
        this.cookies = new HashMap<String, HttpCookie>();
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

    public void addCookie(String domain, String name, String value) {
        HttpCookie cookie = new HttpCookie(url, value);
        cookie.setDomain(domain);
        cookie.setPath("/");
        cookie.setVersion(0);
        cookies.put(url, cookie);
    }

    private void setupConnection(HttpURLConnection conn) {
        conn.setInstanceFollowRedirects(true); // not necessary since it's true by default.
        conn.setRequestProperty("User-Agent", userAgent);

        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);

        try {
            setupCookies();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void setupCookies() throws URISyntaxException {
        // check if the cookie manager is changed!
        if (CookieHandler.getDefault() instanceof CookieManager) {
            CookieManager manager = (CookieManager) CookieManager.getDefault();
            for (Entry<String, HttpCookie> e : cookies.entrySet()) {
                manager.getCookieStore().add(new URI(e.getKey()), e.getValue());
            }
        }
    }
}
