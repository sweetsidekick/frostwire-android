package com.andrew.apollo.appwidgets;

import com.andrew.apollo.MusicPlaybackService;

public class RecentWidgetProvider {

    private static RecentWidgetProvider mInstance;

    public static synchronized RecentWidgetProvider getInstance() {
        if (mInstance == null) {
            mInstance = new RecentWidgetProvider();
        }
        return mInstance;
    }

    public void notifyChange(MusicPlaybackService musicPlaybackService, String what) {
    }
}
