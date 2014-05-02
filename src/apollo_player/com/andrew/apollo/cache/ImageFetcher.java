package com.andrew.apollo.cache;

import android.content.Context;

public class ImageFetcher {

    private static ImageFetcher sInstance;

    public ImageFetcher(Context context) {
    }

    public final static ImageFetcher getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ImageFetcher(context.getApplicationContext());
        }
        return sInstance;
    }

    public void setImageCache(ImageCache cacheCallback) {
    }
}
