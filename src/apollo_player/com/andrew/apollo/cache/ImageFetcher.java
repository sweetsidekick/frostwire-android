package com.andrew.apollo.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

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

    public Bitmap getArtwork(String albumName, long albumId, String artistName) {
        return null;
    }

    public void flush() {
    }

    public void loadCurrentArtwork(ImageView mAlbumArt) {
    }
}
