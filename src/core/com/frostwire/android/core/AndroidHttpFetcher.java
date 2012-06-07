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

package com.frostwire.android.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.net.http.AndroidHttpClient;
import android.util.Log;

/**
 * A Blocking HttpClient.
 * Use fetch() to retrieve the byte[]
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class AndroidHttpFetcher {

    private static final String TAG = "FW.AndroidHttpFetcher";
    
    private static final String DEFAULT_USER_AGENT = UserAgentGenerator.getUserAgent();
    private static final int DEFAULT_TIMEOUT = 5000;

    private static AndroidHttpClient DEFAULT_HTTP_CLIENT;

    private final URI uri;
    private final String userAgent;
    private final int timeout;

    private byte[] body = null;

    static {
        setupHttpClients();
    }

    public AndroidHttpFetcher(URI uri, String userAgent, int timeout) {
        this.uri = uri;
        this.userAgent = userAgent;
        this.timeout = timeout;
    }

    public AndroidHttpFetcher(URI uri, String userAgent) {
        this(uri, userAgent, DEFAULT_TIMEOUT);
    }

    public AndroidHttpFetcher(URI uri, int timeout) {
        this(uri, DEFAULT_USER_AGENT, timeout);
    }

    public AndroidHttpFetcher(URI uri) {
        this(uri, DEFAULT_USER_AGENT);
    }

    public AndroidHttpFetcher(String uri) {
        this(convert(uri));
    }

    public AndroidHttpFetcher(String uri, int timeout) {
        this(convert(uri), timeout);
    }

    public Object[] fetch(boolean gzip) throws IOException {
        HttpHost httpHost = new HttpHost(uri.getHost(), uri.getPort());
        HttpGet httpGet = new HttpGet(uri);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {

            HttpResponse response = DEFAULT_HTTP_CLIENT.execute(httpHost, httpGet);

            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
                throw new IOException("bad status code, downloading file " + response.getStatusLine().getStatusCode());
            }

            Long date = Long.valueOf(0);

            Header[] headers = response.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].getName().startsWith("Last-Modified")) {
                    try {
                        date = DateUtils.parseDate(headers[i].getValue()).getTime();
                    } catch (Exception e) {
                    }
                    break;
                }
            }

            if (response.getEntity() != null) {
                if (gzip) {
                    String str = EntityUtils.toString(response.getEntity());
                    baos.write(str.getBytes());
                } else {
                    response.getEntity().writeTo(baos);
                }
            }

            body = baos.toByteArray();

            if (body == null || body.length == 0) {
                throw new IOException("invalid response");
            }

            return new Object[] { body, date };

        } finally {
            try {
                baos.close();
            } catch (IOException e) {
            }
        }
    }

    public byte[] fetch() {
        Object[] objArray = null;
        try {
            objArray = fetch(false);
        } catch (Throwable e) {
            Log.e(TAG, "Error performing http to " + uri + ", e: " + e.getMessage());
        }

        return objArray != null ? (byte[]) objArray[0] : null;
    }

    public byte[] fetchGzip() {
        Object[] objArray = null;
        try {
            objArray = fetch(true);
        } catch (Throwable e) {
            Log.e(TAG, "Error performing http to " + uri + ", e: " + e.getMessage());
        }

        return objArray != null ? (byte[]) objArray[0] : null;
    }

    public void save(File file) throws IOException {
        save(file, null);
    }

    public void save(File file, HttpFetcherListener listener) throws IOException {
        HttpHost httpHost = new HttpHost(uri.getHost(), uri.getPort());
        HttpGet httpGet = new HttpGet(uri);
        httpGet.addHeader("Connection", "close");

        HttpParams params = httpGet.getParams();
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
        HttpConnectionParams.setStaleCheckingEnabled(params, true);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpClientParams.setRedirecting(params, true);
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpProtocolParams.setUserAgent(params, userAgent);

        FileOutputStream out = null;

        int statusCode = -1;
        Map<String, String> headers = null;

        try {

            out = new FileOutputStream(file);

            HttpResponse response = DEFAULT_HTTP_CLIENT.execute(httpHost, httpGet);

            statusCode = response.getStatusLine().getStatusCode();
            headers = mapHeaders(response.headerIterator());

            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("bad status code, downloading file " + statusCode);
            }

            if (response.getEntity() != null) {
                writeEntity(response.getEntity(), out, listener);
            }

            if (listener != null) {
                listener.onSuccess(new byte[0]);
            }

        } catch (Throwable e) {
            if (listener != null) {
                listener.onError(e, statusCode, headers);
            }
            Log.e(TAG, "Error downloading from: " + uri + ", e: " + e.getMessage());
        } finally {
            out.close();
        }
    }

    public byte[] post(String postBody, String contentType) throws IOException {
        HttpHost httpHost = new HttpHost(uri.getHost(), uri.getPort());
        HttpPost httpPost = new HttpPost(uri);

        StringEntity stringEntity = new StringEntity(postBody);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setEntity(stringEntity);

        HttpParams params = httpPost.getParams();
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
        HttpConnectionParams.setStaleCheckingEnabled(params, true);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpClientParams.setRedirecting(params, true);
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpProtocolParams.setUserAgent(params, DEFAULT_USER_AGENT);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {

            HttpResponse response = DEFAULT_HTTP_CLIENT.execute(httpHost, httpPost);

            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                throw new IOException("bad status code, upload file " + response.getStatusLine().getStatusCode());

            if (response.getEntity() != null) {
                response.getEntity().writeTo(baos);
            }

            body = baos.toByteArray();

            if (body == null || body.length == 0) {
                throw new IOException("invalid response");
            }

            return body;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Http error: " + e.getMessage());
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
            }
        }
    }

    public void post(File file) throws IOException {
        HttpHost httpHost = new HttpHost(uri.getHost(), uri.getPort());
        HttpPost httpPost = new HttpPost(uri);
        FileEntity fileEntity = new FileEntity(file, "binary/octet-stream");
        fileEntity.setChunked(true);
        httpPost.setEntity(fileEntity);

        HttpParams params = httpPost.getParams();
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
        HttpConnectionParams.setStaleCheckingEnabled(params, true);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpClientParams.setRedirecting(params, true);
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpProtocolParams.setUserAgent(params, DEFAULT_USER_AGENT);

        try {

            HttpResponse response = DEFAULT_HTTP_CLIENT.execute(httpHost, httpPost);

            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                throw new IOException("bad status code, upload file " + response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Http error: " + e.getMessage());
        } finally {
            //
        }
    }

    public void post(FileEntity fileEntity) throws IOException {
        HttpHost httpHost = new HttpHost(uri.getHost(), uri.getPort());
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(fileEntity);

        HttpParams params = httpPost.getParams();
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setSoTimeout(params, timeout);
        HttpConnectionParams.setStaleCheckingEnabled(params, true);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpClientParams.setRedirecting(params, true);
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpProtocolParams.setUserAgent(params, DEFAULT_USER_AGENT);

        try {

            HttpResponse response = DEFAULT_HTTP_CLIENT.execute(httpHost, httpPost);

            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                throw new IOException("bad status code, upload file " + response.getStatusLine().getStatusCode());

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Http error: " + e.getMessage());
        } finally {
            //
        }
    }

    private static void setupHttpClients() {
        DEFAULT_HTTP_CLIENT = AndroidHttpClient.newInstance(DEFAULT_USER_AGENT);
    }

    private static void writeEntity(final HttpEntity entity, OutputStream out, HttpFetcherListener listener) throws IOException {
        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        InputStream in = entity.getContent();
        if (in == null) {
            return;
        }
        int i = (int) entity.getContentLength();
        if (i < 0) {
            i = 4096;
        }
        try {
            byte[] data = new byte[4096];
            int n;
            while ((n = in.read(data)) != -1) {
                if (listener != null) {
                    listener.onData(data, n);
                }
                out.write(data, 0, n);
                out.flush();
            }
        } finally {
            in.close();
        }
    }

    private static URI convert(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Map<String, String> mapHeaders(HeaderIterator it) {
        Map<String, String> map = new HashMap<String, String>();

        while (it.hasNext()) {
            Header h = it.nextHeader();
            map.put(h.getName(), h.getValue());
        }

        return map;
    }
}
