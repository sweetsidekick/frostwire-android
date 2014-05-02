package com.andrew.apollo.appwidgets;

import com.andrew.apollo.MusicPlaybackService;

public class AppWidgetLarge {

    private static AppWidgetLarge mInstance;

    public static synchronized AppWidgetLarge getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetLarge();
        }
        return mInstance;
    }

    public void notifyChange(MusicPlaybackService musicPlaybackService, String what) {
    }
}
