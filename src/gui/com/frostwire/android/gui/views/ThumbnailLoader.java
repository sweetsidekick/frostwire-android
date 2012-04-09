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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.frostwire.android.core.Constants;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public final class ThumbnailLoader {

    private final LruCache<Integer, Bitmap> cache;
    private final Map<ImageView, Integer> imageViews;
    private final ExecutorService executorService;

    private final int fileType;
    private final Drawable defaultDrawable;

    public ThumbnailLoader(int fileType, Drawable defaultDrawable) {
        this.cache = new LruCache<Integer, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8));
        this.imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, Integer>());
        this.executorService = Executors.newFixedThreadPool(5);

        this.fileType = fileType;
        this.defaultDrawable = defaultDrawable;
    }

    public void displayImage(Integer key, ImageView imageView) {
        imageViews.put(imageView, key);
        Bitmap bitmap = cache.get(key);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            queueThumbnail(key, imageView);
            imageView.setImageDrawable(defaultDrawable);
        }
    }

    public void clearCache() {
        cache.evictAll();
    }

    private void queueThumbnail(Integer key, ImageView imageView) {
        ThumbnailToLoad p = new ThumbnailToLoad(key, imageView);
        executorService.submit(new ThumbnailLoaderTask(p));
    }

    private Bitmap getBitmap(Context context, Integer key) {
        Bitmap bmp = null;

        try {
            ContentResolver cr = context.getContentResolver();

            if (fileType == Constants.FILE_TYPE_PICTURES) {
                bmp = Images.Thumbnails.getThumbnail(cr, key, Images.Thumbnails.MICRO_KIND, null);
            } else if (fileType == Constants.FILE_TYPE_VIDEOS) {
                bmp = Video.Thumbnails.getThumbnail(cr, key, Video.Thumbnails.MICRO_KIND, null);
            }
        } catch (Throwable e) {
            // ignore
        }

        return bmp;
    }

    private boolean imageViewReused(ThumbnailToLoad thumbnailToLoad) {
        Integer key = imageViews.get(thumbnailToLoad.imageView);
        if (key == null || !key.equals(thumbnailToLoad.key)) {
            return true;
        }
        return false;
    }

    private static final class ThumbnailToLoad {

        public final Integer key;
        public final ImageView imageView;

        public ThumbnailToLoad(Integer key, ImageView i) {
            this.key = key;
            imageView = i;
        }
    }

    private final class ThumbnailLoaderTask implements Runnable {

        private final ThumbnailToLoad thumbnailToLoad;

        public ThumbnailLoaderTask(ThumbnailToLoad thumbnailToLoad) {
            this.thumbnailToLoad = thumbnailToLoad;
        }

        @Override
        public void run() {
            Activity a = (Activity) thumbnailToLoad.imageView.getContext();

            if (imageViewReused(thumbnailToLoad)) {
                return;
            }

            Bitmap bmp = getBitmap(a, thumbnailToLoad.key);
            cache.put(thumbnailToLoad.key, bmp);

            if (imageViewReused(thumbnailToLoad)) {
                return;
            }

            BitmapDisplayer bd = new BitmapDisplayer(bmp, thumbnailToLoad);

            a.runOnUiThread(bd);
        }
    }

    private final class BitmapDisplayer implements Runnable {

        private final Bitmap bitmap;
        private final ThumbnailToLoad thumbnailToLoad;

        public BitmapDisplayer(Bitmap b, ThumbnailToLoad p) {
            bitmap = b;
            thumbnailToLoad = p;
        }

        @Override
        public void run() {
            if (imageViewReused(thumbnailToLoad)) {
                return;
            }

            if (bitmap != null) {
                thumbnailToLoad.imageView.setImageBitmap(bitmap);
            } else {
                thumbnailToLoad.imageView.setImageDrawable(defaultDrawable);
            }
        }
    }
}
