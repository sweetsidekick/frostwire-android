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

package com.frostwire.android.gui.views;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.apache.commons.io.IOUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.core.providers.UniversalStore.Applications;
import com.frostwire.android.gui.util.DiskLruRawDataCache;
import com.frostwire.android.gui.util.MusicUtils;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.util.FileUtils;
import com.squareup.picasso.Loader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.Builder;
import com.squareup.picasso.RequestBuilder;
import com.squareup.picasso.Transformation;
import com.squareup.picasso.UrlConnectionLoader;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public final class ImageLoader {

    private static final int MEMORY_CACHE_SIZE = 1024 * 1024 * 2; // 2MB
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    
    public static final int OVERLAY_FLAG_PLAY = 1;

    private final DiskLruRawDataCache diskCache;

    private final Context context;

    private static ImageLoader defaultInstance;

    private final Picasso picasso;

    public static ImageLoader getDefault() {
        return defaultInstance;
    }

    public static void createDefaultInstance(Context context) {
        if (defaultInstance == null) {
            defaultInstance = new ImageLoader(context);
        }
    }

    public ImageLoader(Context context) {
        this.context = context;
        diskCache = diskCacheOpen();
        picasso = new Builder(context).loader(new ThumbnailLoader()).memoryCache(new LruCache(MEMORY_CACHE_SIZE)).build();
        picasso.setDebugging(false);
    }

    private DiskLruRawDataCache diskCacheOpen() {
        DiskLruRawDataCache cache = null;
        File imgCacheDir = SystemUtils.getImageCacheDirectory();
        cache = (FileUtils.isValidDirectory(imgCacheDir)) ? new DiskLruRawDataCache(imgCacheDir, DISK_CACHE_SIZE) : null;
        return cache;
    }

    public void displayImage(FileDescriptor image, ImageView imageView, Drawable defaultDrawable) {
        displayImage(image, imageView, defaultDrawable, 0);
    }

    public void displayImage(FileDescriptor fd, ImageView imageView, Drawable defaultDrawable, int overlayFlags) {
        StringBuilder path = getResourceIdentifier(fd);
        displayImage(path.toString(), imageView, defaultDrawable, overlayFlags);
    }

    /**
     * @param fd
     * @return Depending on the file type returns either video:<id> or image:<id>
     */
    private StringBuilder getResourceIdentifier(FileDescriptor fd) {
        StringBuilder path = new StringBuilder();

        switch (fd.fileType) {
        case Constants.FILE_TYPE_PICTURES:
            path.append("image:");
            break;
        case Constants.FILE_TYPE_VIDEOS:
            path.append("video:");
            break;
        case Constants.FILE_TYPE_AUDIO:
            path.append("audio:");
            break;
        case Constants.FILE_TYPE_APPLICATIONS:
            path.append("application:");
            break;
        default:
            path.append("image:");
            break;
        }
        path.append(fd.id);
        return path;
    }

    public void displayImage(String imageSrc, ImageView imageView, Drawable defaultDrawable, int overlayFlags) {
        if (defaultDrawable != null) {
            imageView.setScaleType(ScaleType.FIT_CENTER);
            
            RequestBuilder requestBuilder = picasso.load(imageSrc).placeholder(defaultDrawable);
            
            if ((overlayFlags & OVERLAY_FLAG_PLAY) == OVERLAY_FLAG_PLAY) {
                requestBuilder.transform(new OverlayBitmapTransformation(imageView,imageSrc,R.drawable.play_icon_transparent,40,40));
            }
            
            requestBuilder.into(imageView);
        }
    }

    private boolean isKeyRemote(String key) {
        return key.startsWith("http://");
    }

    private Bitmap getBitmap(Context context, byte fileType, long id) {
        Bitmap bmp = null;

        try {
            ContentResolver cr = context.getContentResolver();

            if (fileType == Constants.FILE_TYPE_PICTURES) {
                bmp = Images.Thumbnails.getThumbnail(cr, id, Images.Thumbnails.MICRO_KIND, null);
            } else if (fileType == Constants.FILE_TYPE_VIDEOS) {
                bmp = Video.Thumbnails.getThumbnail(cr, id, Video.Thumbnails.MICRO_KIND, null);
            } else if (fileType == Constants.FILE_TYPE_AUDIO) {
                bmp = MusicUtils.getArtwork(context, id, -1);
            } else if (fileType == Constants.FILE_TYPE_APPLICATIONS) {
                InputStream is = cr.openInputStream(Uri.withAppendedPath(Applications.Media.CONTENT_URI_ITEM, String.valueOf(id)));
                bmp = BitmapFactory.decodeStream(is);
                is.close();
            }
        } catch (Throwable e) {
            bmp = null;
            // ignore
        }

        return bmp;
    }

    private class RawDataResponse extends Loader.Response {

        private final byte[] data;

        public RawDataResponse(byte[] data, boolean loadedFromCache) {
            super(new ByteArrayInputStream(data), loadedFromCache);
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }
    }

    /**
     * We'll use the Raw data response given by this loader to store bytes
     * on a PRIVATE disk cache that nobody else will come and erase or share with us,
     * like the default HttpResponseCache that Picasso uses (which doesn't work for
     * older androids, and which I believe sucks balls)
     * @author gubatron
     *
     */
    private class RawDataUrlConnectionLoader extends UrlConnectionLoader {

        public RawDataUrlConnectionLoader(Context context) {
            super(context);
        }

        @Override
        public RawDataResponse load(String url, boolean localCacheOnly) throws IOException {
            HttpURLConnection connection = openConnection(url);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(connection.getInputStream(), baos);
            connection.disconnect();
            return new RawDataResponse(baos.toByteArray(), localCacheOnly);
        }
    }

    private class ThumbnailLoader implements Loader {

        private final RawDataUrlConnectionLoader fallback;

        public ThumbnailLoader() {
            fallback = new RawDataUrlConnectionLoader(context);
        }

        /**
         * @param itemIdentifier video:<videoId>, or image:<imageId>, audio:<audioId>, where the Id is an Integer.
         */
        @Override
        public Response load(String itemIdentifier, boolean localCacheOnly) throws IOException {
            Response response = null;

            byte fileType = getFileType(itemIdentifier);

            if (fileType != -1) {
                response = fromFileType(itemIdentifier, localCacheOnly, fileType);
            } else if (isKeyRemote(itemIdentifier)) {
                response = fromRemote(itemIdentifier, localCacheOnly);
            } else {
                response = fallback.load(itemIdentifier, localCacheOnly);
            }

            return response;
        }

        private Response fromRemote(String itemIdentifier, boolean localCacheOnly) throws IOException {
            RawDataResponse response = null;
            
            if (diskCache != null) {
                if (!diskCache.containsKey(itemIdentifier)) {
                    response = fallback.load(itemIdentifier, localCacheOnly);
                    diskCache.put(itemIdentifier, response.getData());
                } else {
                    byte[] data = diskCache.getBytes(itemIdentifier);
                    response = new RawDataResponse(data,localCacheOnly);
                }
            } else {
                response = fallback.load(itemIdentifier, false);
            }

            return response;
        }
        
        private Response fromFileType(String itemIdentifier, boolean localCacheOnly, byte fileType) throws IOException {
            Response response;
            long id = getFileId(itemIdentifier);
            
            Bitmap bitmap = getBitmap(context, fileType, id);

            if (bitmap == null) {
                throw new IOException("ThumbnailLoader - bitmap not found.");
            }

            response = new Response(convertToStream(bitmap), localCacheOnly);
            return response;
        }

        private InputStream convertToStream(Bitmap bitmap) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(getByteCount(bitmap));
            bitmap.compress(CompressFormat.PNG, 100, baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return bais;
        }
        
        private int getByteCount(final Bitmap bitmap) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            } else {
                return bitmap.getByteCount();
            }
        }
        
        private byte getFileType(String itemIdentifier) {
            byte fileType = -1;

            if (itemIdentifier.startsWith("image:")) {
                fileType = Constants.FILE_TYPE_PICTURES;
            } else if (itemIdentifier.startsWith("video:")) {
                fileType = Constants.FILE_TYPE_VIDEOS;
            } else if (itemIdentifier.startsWith("audio:")) {
                fileType = Constants.FILE_TYPE_AUDIO;
            } else if (itemIdentifier.startsWith("application:")) {
                fileType = Constants.FILE_TYPE_APPLICATIONS;
            }
            return fileType;
        }

        private long getFileId(String itemIdentifier) {
            return Long.valueOf(itemIdentifier.substring(itemIdentifier.indexOf(':')+1));
        }
    }
    
    private class OverlayBitmapTransformation implements Transformation {
        private final ImageView imageView;
        private final String keyPrefix;
        private final int overlayIconId;
        private final int overlayWidthPercentage;
        private final int overlayHeightPercentage;
        
        public OverlayBitmapTransformation(ImageView imageView, String keyPrefix, int overlayIconId, int overlayWidthPercentage, int overlayHeightPercentage) {
            this.imageView = imageView;
            this.keyPrefix = keyPrefix;
            this.overlayIconId = overlayIconId;
            this.overlayWidthPercentage = overlayWidthPercentage;
            this.overlayHeightPercentage = overlayHeightPercentage;
        }
        
        @Override
        public Bitmap transform(Bitmap source) {
            Bitmap bmp = overlayIcon(source, overlayIconId, overlayWidthPercentage, overlayHeightPercentage);
            source.recycle();
            return bmp;
        }

        @Override
        public String key() {
            return keyPrefix + ":" + overlayIconId + ":" + imageView.getWidth() + "," + imageView.getHeight() + ":" + overlayWidthPercentage + "," + overlayHeightPercentage;
        }
        
        private Bitmap overlayIcon(Bitmap backgroundBmp, int iconResId, int iconWidthPercentage, int iconHeightPercentage) {
            Bitmap canvasBitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), backgroundBmp.getConfig());
            Canvas canvas = resizeBackgroundToFitImageView(canvasBitmap,backgroundBmp);
            paintScaledIcon(canvas, iconResId, iconWidthPercentage, iconHeightPercentage);
            return canvasBitmap;
        }

        private void paintScaledIcon(Canvas canvas, int iconResId, int iconWidthPercentage, int iconHeightPercentage) {
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), iconResId);
            Rect iconSrcRect = new Rect(0, 0, icon.getWidth(), icon.getHeight());
            int iconResizedWidth = (int) ((imageView.getWidth() * iconWidthPercentage) / 100f);
            int iconResizedHeight = (int) ((imageView.getHeight() * iconHeightPercentage) / 100f);
            int left = (imageView.getWidth() - iconResizedWidth) >> 1;
            int top = (imageView.getHeight() - iconResizedHeight) >> 1;
            Rect iconDestRect = new Rect(left, top, left + iconResizedWidth, top + iconResizedHeight);
            canvas.drawBitmap(icon, iconSrcRect, iconDestRect, null);
        }

        private Canvas resizeBackgroundToFitImageView(Bitmap canvasBitmap, Bitmap backgroundBmp) {
            Rect backgroundDestRect = new Rect(0,0,imageView.getWidth(), imageView.getHeight());
            Canvas canvas = new Canvas(canvasBitmap);
            canvas.drawBitmap(backgroundBmp, null, backgroundDestRect, null);
            return canvas;
        }
    }
}