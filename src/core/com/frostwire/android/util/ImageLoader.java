/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

package com.frostwire.android.util;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.Builder;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class ImageLoader {

    private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

    private static final String SCHEME_IMAGE = "image";

    private static final String SCHEME_IMAGE_SLASH = SCHEME_IMAGE + "://";

    private static final String APPLICATION_AUTHORITY = "application";

    private static final String ALBUM_AUTHORITY = "album";

    public static final Uri APPLICATION_THUMBNAILS_URI = Uri.parse(SCHEME_IMAGE_SLASH + APPLICATION_AUTHORITY);

    public static final Uri ALBUM_THUMBNAILS_URI = Uri.parse(SCHEME_IMAGE_SLASH + ALBUM_AUTHORITY);

    private final ImageCache cache;
    private final Picasso picasso;

    private static ImageLoader instance;

    public final static ImageLoader getInstance(Context context) {
        if (instance == null) {
            instance = new ImageLoader(context);
        }
        return instance;
    }
    
    /**
     * WARNING: this method does not make use of the cache.
     * it is here to be used only (so far) on the notification window view and the RC Interface (things like Lock Screen, Android Wear),
     * which run on another process space. If you try to use a cached image there, you will get some
     * nasty exceptions, therefore you will need this.
     * 
     * For loading album art inside the application Activities/Views/Fragments, take a look at FileListAdapter and how it uses the ImageLoader.
     * 
     * @param context
     * @param albumId
     * @return
     */
    public static Bitmap getAlbumArt(Context context, String albumId) {
        Bitmap bitmap = null;
        Cursor cursor = context.getContentResolver().query(Uri.withAppendedPath(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId), new String[] { MediaStore.Audio.AlbumColumns.ALBUM_ART }, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                String albumArt = cursor.getString(0);
                bitmap = BitmapFactory.decodeFile(albumArt);
            }
        } finally {
            cursor.close();
        }

        return bitmap;
    }


    private ImageLoader(Context context) {
        File directory = SystemUtils.getCacheDir(context, "picasso");
        long diskSize = SystemUtils.calculateDiskCacheSize(directory, MIN_DISK_CACHE_SIZE, MAX_DISK_CACHE_SIZE);
        int memSize = SystemUtils.calculateMemoryCacheSize(context);

        this.cache = new ImageCache(directory, diskSize, memSize);
        this.picasso = new Builder(context).downloader(new ImageDownloader(context.getApplicationContext())).memoryCache(cache).build();

        picasso.setIndicatorsEnabled(false);
    }

    public void load(Uri uri, ImageView target) {
        picasso.load(uri).noFade().into(target);
    }

    public void load(Uri uri, ImageView target, int targetWidth, int targetHeight) {
        picasso.load(uri).noFade().resize(targetWidth, targetHeight).into(target);
    }

    public void load(Uri uri, ImageView target, int placeholderResId) {
        picasso.load(uri).noFade().placeholder(placeholderResId).into(target);
    }

    public void load(Uri uri, ImageView target, int targetWidth, int targetHeight, int placeholderResId) {
        picasso.load(uri).noFade().resize(targetWidth, targetHeight).placeholder(placeholderResId).into(target);
    }

    public Bitmap get(Uri uri) {
        try {
            return picasso.load(uri).get();
        } catch (IOException e) {
            return null;
        }
    }

    public void clear() {
        cache.clear();
    }

    private static class ImageDownloader implements Downloader {

        private final PackageApplicationDownloader appDownloader;
        private final AlbumDownloader albumDownloader;
        private final UrlConnectionDownloader urlDownloader;

        public ImageDownloader(Context context) {
            this.appDownloader = new PackageApplicationDownloader(context);
            this.albumDownloader = new AlbumDownloader(context);
            this.urlDownloader = new UrlConnectionDownloader();
        }

        @Override
        public Response load(Uri uri, boolean localCacheOnly) throws IOException {
            Downloader downloader = null;

            String scheme = uri.getScheme();

            if (SCHEME_IMAGE.equals(scheme)) {
                String authority = uri.getAuthority();

                if (APPLICATION_AUTHORITY.equals(authority)) {
                    downloader = appDownloader;
                } else if (ALBUM_AUTHORITY.equals(authority)) {
                    downloader = albumDownloader;
                }
            } else {
                downloader = urlDownloader;
            }

            return downloader != null ? downloader.load(uri, localCacheOnly) : null;
        }
    }

    private static class PackageApplicationDownloader implements Downloader {

        private final Context context;

        public PackageApplicationDownloader(Context context) {
            this.context = context;
        }

        @Override
        public Response load(Uri uri, boolean localCacheOnly) throws IOException {
            Response response = null;
            String packageName = uri.getLastPathSegment();

            PackageManager pm = context.getPackageManager();
            try {
                BitmapDrawable icon = (BitmapDrawable) pm.getApplicationIcon(packageName);
                Bitmap bmp = icon.getBitmap();

                response = new Response(bmp, false, bmp.getByteCount());
            } catch (NameNotFoundException e) {
                response = null;
            }
            return response;
        }
    }

    private static class AlbumDownloader implements Downloader {

        private final Context context;

        public AlbumDownloader(Context context) {
            this.context = context;
        }

        @Override
        public Response load(Uri uri, boolean localCacheOnly) throws IOException {
            String albumId = uri.getLastPathSegment();
            Bitmap bitmap = getAlbumArt(context, albumId);
            return (bitmap != null) ? new Response(bitmap, false, bitmap.getByteCount()) : null;
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

            long contentLength = connection.getHeaderFieldInt("Content-Length", -1);
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
