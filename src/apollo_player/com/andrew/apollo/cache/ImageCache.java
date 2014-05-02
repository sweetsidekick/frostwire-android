package com.andrew.apollo.cache;

import android.content.Context;

public class ImageCache {

    private static ImageCache sInstance;

    public ImageCache(Context context) {
    }

    public final static ImageCache getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ImageCache(context.getApplicationContext());
        }
        return sInstance;
    }
}
