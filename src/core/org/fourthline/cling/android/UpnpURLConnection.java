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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class UpnpURLConnection extends HttpURLConnection {

    private static final String[] methods = { "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "SUBSCRIBE", "UNSUBSCRIBE", "NOTIFY" };

    private HttpURLConnection conn;

    protected UpnpURLConnection(HttpURLConnection conn, URL url) {
        super(url);
        this.conn = conn;
    }

    @Override
    public final void connect() throws IOException {
        conn.connect();
    }

    @Override
    public final void disconnect() {
        conn.disconnect();
    }

    /**
     * Returns an input stream from the server in the case of error such as the
     * requested file (txt, htm, html) is not found on the remote server.
     */
    @Override
    public final InputStream getErrorStream() {
        return conn.getErrorStream();
    }

    /**
     * Returns the value of the field at {@code position}. Returns null if there
     * are fewer than {@code position} headers.
     */
    @Override
    public final String getHeaderField(int pos) {
        return conn.getHeaderField(pos);
    }

    /**
     * Returns the value of the field corresponding to the {@code fieldName}, or
     * null if there is no such field. If the field has multiple values, the
     * last value is returned.
     */
    @Override
    public final String getHeaderField(String key) {
        return conn.getHeaderField(key);
    }

    @Override
    public final String getHeaderFieldKey(int posn) {
        return conn.getHeaderFieldKey(posn);
    }

    @Override
    public final Map<String, List<String>> getHeaderFields() {
        return conn.getHeaderFields();
    }

    @Override
    public final Map<String, List<String>> getRequestProperties() {
        return conn.getRequestProperties();
    }

    @Override
    public final InputStream getInputStream() throws IOException {
        return conn.getInputStream();
    }

    @Override
    public final OutputStream getOutputStream() throws IOException {
        return conn.getOutputStream();
    }

    @Override
    public final Permission getPermission() throws IOException {
        return conn.getPermission();
    }

    @Override
    public final String getRequestProperty(String field) {
        return conn.getRequestProperty(field);
    }

    @Override
    public final boolean usingProxy() {
        return conn.usingProxy();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return conn.getResponseMessage();
    }

    @Override
    public final int getResponseCode() throws IOException {
        return conn.getResponseCode();
    }

    @Override
    public final void setRequestProperty(String field, String newValue) {
        conn.setRequestProperty(field, newValue);
    }

    @Override
    public final void addRequestProperty(String field, String newValue) {
        conn.addRequestProperty(field, newValue);
    }

    @Override
    public void setRequestMethod(String method) throws ProtocolException {
        if (connected) {
            throw new ProtocolException("Cannot reset method once connected");
        }
        for (String m : methods) {
            if (m.equals(method)) {
                this.method = method;
                setConnectionRequestMethod(method);
                return;
            }
        }

        throw new ProtocolException("Invalid UPnP HTTP method: " + method);
    }

    private void setConnectionRequestMethod(String method) throws ProtocolException {
        // see if the method supports output
        if (method.equals("PUT") || method.equals("POST") || method.equals("NOTIFY")) {
            // fake the method so the inner connection method sets its instance variables
            method = "PUT";
        } else {
            // use any method that doesn't support output, an exception will be
            // raised by the superclass
            method = "GET";
        }
        conn.setRequestMethod(method);
    }
}
