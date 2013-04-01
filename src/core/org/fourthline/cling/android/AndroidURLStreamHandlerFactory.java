/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
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

package org.fourthline.cling.android;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class AndroidURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private static URLStreamHandler platformHandler;
    private static Method openConnectionM;

    static {
        try {
            URL u = new URL("http://probe");
            Field f = URL.class.getDeclaredField("streamHandler");
            f.setAccessible(true);

            platformHandler = (URLStreamHandler) f.get(u);

            openConnectionM = URLStreamHandler.class.getDeclaredMethod("openConnection", URL.class);
            openConnectionM.setAccessible(true);
        } catch (Throwable e) {
            throw new RuntimeException("Faulty platform", e);
        }
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("http".equals(protocol)) {
            return new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                    HttpURLConnection conn = platformOpenConnection(u);
                    return new UpnpURLConnection(conn, u);
                }
            };
        } else {
            return null;
        }
    }

    private static HttpURLConnection platformOpenConnection(URL url) throws IOException {
        try {
            return (HttpURLConnection) openConnectionM.invoke(platformHandler, url);
        } catch (Throwable e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new RuntimeException("Faulty platform", e);
            }
        }
    }
}
