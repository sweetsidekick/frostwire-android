package com.andrew.apollo.appwidgets;

import com.andrew.apollo.MusicPlaybackService;

public class AppWidgetLargeAlternate {

    private static AppWidgetLargeAlternate mInstance;

    public static synchronized AppWidgetLargeAlternate getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetLargeAlternate();
        }
        return mInstance;
    }

    public void notifyChange(MusicPlaybackService musicPlaybackService, String what) {
    }
}
