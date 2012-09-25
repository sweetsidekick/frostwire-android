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
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public final class ThumbnailLoader {

    private final LruCache<Integer, Bitmap> cache;

    private final Context context;
    private final int fileType;
    private final Drawable defaultDrawable;

    public ThumbnailLoader(Context context, int fileType, Drawable defaultDrawable) {
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

        this.fileType = fileType;
        this.defaultDrawable = defaultDrawable;
    }

    public void displayImage(Integer key, ImageView imageView) {
        Bitmap bitmap = cache.get(key);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageDrawable(defaultDrawable);
            queueThumbnail(key, imageView);
        }
    }

    public void clearCache() {
        cache.evictAll();
    }

    private void queueThumbnail(Integer key, ImageView imageView) {
        ThumbnailToLoad p = new ThumbnailToLoad(key, imageView);
        BitmapWorkerTask task = new BitmapWorkerTask(p);
        task.execute();
    }

    private Bitmap getBitmap(Context context, Integer key) {
        Bitmap bmp = null;

        try {
            ContentResolver cr = context.getContentResolver();

            if (fileType == Constants.FILE_TYPE_PICTURES) {
                bmp = Images.Thumbnails.getThumbnail(cr, key, Images.Thumbnails.MICRO_KIND, null);
            } else if (fileType == Constants.FILE_TYPE_VIDEOS) {
                bmp = Video.Thumbnails.getThumbnail(cr, key, Video.Thumbnails.MICRO_KIND, null);
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

        Drawable playIcon = context.getResources().getDrawable(R.drawable.play_icon_transparent);
        playIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        playIcon.draw(canvas);

        return bitmap;
    }

    private static final class ThumbnailToLoad {

        public final Integer key;
        public final ImageView imageView;

        public ThumbnailToLoad(Integer key, ImageView imageView) {
            this.key = key;
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
            Bitmap bmp = getBitmap(context, thumbnailToLoad.key);
            if (bmp != null) {
                cache.put(thumbnailToLoad.key, bmp);
            }

            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                thumbnailToLoad.imageView.setImageBitmap(bitmap);
            } else {
                thumbnailToLoad.imageView.setImageDrawable(defaultDrawable);
            }
        }
    }
}
