package com.frostwire.android.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.Builder;

public class ImageLoader {

    private final Picasso picasso;

    public ImageLoader(Context context) {
        picasso = new Builder(context).downloader(new ImageDownloader()).build();
    }

    public void load(Uri uri, ImageView target) {
        picasso.load(uri).noFade().into(target);
    }

    private static class ImageDownloader implements Downloader {

        @Override
        public Response load(Uri uri, boolean localCacheOnly) throws IOException {
            String scheme = uri.getScheme();
            Downloader downloader = null;

            if (scheme.equals("http")) {
                downloader = new UrlConnectionDownloader();
            } else if (scheme.equals("content")) {
                //
            }

            return downloader != null ? downloader.load(uri, localCacheOnly) : null;
        }
    }

    private static class UrlConnectionDownloader implements Downloader {

        private static final int DEFAULT_READ_TIMEOUT = 20 * 1000; // 20s
        private static final int DEFAULT_CONNECT_TIMEOUT = 15 * 1000; // 15s

        private static final String RESPONSE_SOURCE = "X-Android-Response-Source";

        public UrlConnectionDownloader() {
        }

        @Override
        public Response load(Uri uri, boolean localCacheOnly) throws IOException {
            HttpURLConnection connection = openConnection(uri);
            connection.setUseCaches(true);
            if (localCacheOnly) {
                connection.setRequestProperty("Cache-Control", "only-if-cached,max-age=" + Integer.MAX_VALUE);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 300) {
                connection.disconnect();
                throw new ResponseException(responseCode + " " + connection.getResponseMessage());
            }

            long contentLength = connection.getHeaderFieldInt("Content-Length", 0);
            boolean fromCache = parseResponseSourceHeader(connection.getHeaderField(RESPONSE_SOURCE));

            return new Response(connection.getInputStream(), fromCache, contentLength);
        }

        protected HttpURLConnection openConnection(Uri path) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(path.toString()).openConnection();
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            return connection;
        }

        /** Returns {@code true} if header indicates the response body was loaded from the disk cache. */
        private static boolean parseResponseSourceHeader(String header) {
            if (header == null) {
                return false;
            }
            String[] parts = header.split(" ", 2);
            if ("CACHE".equals(parts[0])) {
                return true;
            }
            if (parts.length == 1) {
                return false;
            }
            try {
                return "CONDITIONAL_CACHE".equals(parts[0]) && Integer.parseInt(parts[1]) == 304;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
