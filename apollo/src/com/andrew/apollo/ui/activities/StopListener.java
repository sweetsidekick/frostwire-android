package com.andrew.apollo.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;
import android.view.View;
import com.andrew.apollo.utils.MusicUtils;
import com.frostwire.android.core.Constants;
import com.frostwire.util.Ref;

import java.lang.ref.WeakReference;

final class StopListener implements View.OnLongClickListener {
    private WeakReference<Activity> activityRef;
    private final boolean finishOnStop;

    public StopListener(Activity activity, boolean finishOnStop) {
        this.activityRef = Ref.weak(activity);
        this.finishOnStop = finishOnStop;
    }

    @Override
    public boolean onLongClick(View v) {
        try {
            MusicUtils.mService.stop();
            if (Ref.alive(activityRef)) {
                v.getContext().sendBroadcast(new Intent(Constants.ACTION_MEDIA_PLAYER_STOPPED));

                if (finishOnStop) {
                    activityRef.get().onBackPressed();
                }
            }
        } catch (RemoteException e) {
            // ignore
        }
        return true;
    }
};