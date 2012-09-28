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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.support.v4.util.LruCache;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public final class ThumbnailLoader {

    private final LruCache<Integer, Bitmap> cache;

    private final Context context;

    public ThumbnailLoader(Context context) {
        this.context = context;

        // code taken from http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
        // Get memory class of this device, exceeding this amount will throw an
        // OutOfMemory exception.
        final int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = 1024 * 1024 * memClass / 8;

        this.cache = new LruCache<Integer, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, Bitmap bitmap) {
                // The cache size will be measured in bytes rather than number of items.
                //return bitmap.getByteCount();
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
    }

    public void displayImage(FileDescriptor fd, ImageView imageView, Drawable defaultDrawable) {
        Bitmap bitmap = cache.get(fd.hashCode());
        if (bitmap != null) {
            imageView.setScaleType(ScaleType.FIT_CENTER);
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setScaleType(ScaleType.CENTER);
            imageView.setImageDrawable(defaultDrawable);
            queueThumbnail(fd, imageView);
        }
    }

    public void clearCache() {
        cache.evictAll();
    }

    private void queueThumbnail(FileDescriptor fd, ImageView imageView) {
        ThumbnailToLoad p = new ThumbnailToLoad(fd, imageView);
        BitmapWorkerTask task = new BitmapWorkerTask(p);
        task.execute();
    }

    private Bitmap getBitmap(Context context, FileDescriptor fd) {
        Bitmap bmp = null;

        try {
            ContentResolver cr = context.getContentResolver();

            if (fd.fileType == Constants.FILE_TYPE_PICTURES) {
                bmp = Images.Thumbnails.getThumbnail(cr, fd.id, Images.Thumbnails.MICRO_KIND, null);
            } else if (fd.fileType == Constants.FILE_TYPE_VIDEOS) {
                bmp = Video.Thumbnails.getThumbnail(cr, fd.id, Video.Thumbnails.MICRO_KIND, null);
                bmp = overlayVideoIcon(context, bmp);
            }
        } catch (Throwable e) {
            // ignore
        }

        return bmp;
    }

    private Bitmap overlayVideoIcon(Context context, Bitmap bmp) {
        Bitmap bitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        Canvas canvas = new Canvas(bitmap);

        canvas.drawBitmap(bmp, 0, 0, null);

        Bitmap playIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.play_icon_transparent);
        Rect src = new Rect(0, 0, playIcon.getWidth(), playIcon.getHeight());
        int dx = (bmp.getWidth() - src.width()) / 2;
        int dy = (bmp.getHeight() - src.height()) / 2;
        Rect dst = new Rect(dx, dy, src.width() + dx, src.height() + dy);
        canvas.drawBitmap(playIcon, src, dst, null);

        return bitmap;
    }

    private static final class ThumbnailToLoad {

        public final FileDescriptor fd;
        public final ImageView imageView;

        public ThumbnailToLoad(FileDescriptor fd, ImageView imageView) {
            this.fd = fd;
            this.imageView = imageView;
        }
    }

    private class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {

        private final ThumbnailToLoad thumbnailToLoad;

        public BitmapWorkerTask(ThumbnailToLoad thumbnailToLoad) {
            this.thumbnailToLoad = thumbnailToLoad;
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            Bitmap bmp = getBitmap(context, thumbnailToLoad.fd);
            if (bmp != null) {
                cache.put(thumbnailToLoad.fd.hashCode(), bmp);
            }

            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                thumbnailToLoad.imageView.setScaleType(ScaleType.FIT_CENTER);
                thumbnailToLoad.imageView.setImageBitmap(bitmap);
            }
        }
    }
}
