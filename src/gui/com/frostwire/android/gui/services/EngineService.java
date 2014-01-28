/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.services;

import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.player.CoreMediaPlayer;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.httpserver.HttpServerManager;
import com.frostwire.android.gui.transfers.AzureusManager;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.util.ByteUtils;
import com.frostwire.android.util.concurrent.ThreadPool;
import com.frostwire.localpeer.AndroidMulticastLock;
import com.frostwire.localpeer.LocalPeer;
import com.frostwire.localpeer.LocalPeerManager;
import com.frostwire.localpeer.LocalPeerManagerImpl;
import com.frostwire.localpeer.LocalPeerManagerListener;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class EngineService extends Service implements IEngineService {

    private static final String TAG = "FW.EngineService";

    private final static long[] VENEZUELAN_VIBE = buildVenezuelanVibe();

    private final IBinder binder;

    private final ThreadPool threadPool;

    // services in background
    

    private final CoreMediaPlayer mediaPlayer;

    private byte state;

    private OnSharedPreferenceChangeListener preferenceListener;

    public EngineService() {
        binder = new EngineServiceBinder();

        threadPool = new ThreadPool("Engine");

        mediaPlayer = new NativeAndroidPlayer(this);

        registerPreferencesChangeListener();

        state = STATE_DISCONNECTED;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        stopServices(false);

        mediaPlayer.stop();
    }

    public CoreMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public byte getState() {
        return state;
    }

    public boolean isStarted() {
        return getState() == STATE_STARTED;
    }

    public boolean isStarting() {
        return getState() == STATE_STARTING;
    }

    public boolean isStopped() {
        return getState() == STATE_STOPPED;
    }

    public boolean isStopping() {
        return getState() == STATE_STOPPING;
    }

    public boolean isDisconnected() {
        return getState() == STATE_DISCONNECTED;
    }

    public synchronized void startServices() {
        // hard check for TOS
        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
            return;
        }

        if (!Librarian.instance().isExternalStorageMounted()) {
            return;
        }

        if (isStarted() || isStarting()) {
            return;
        }

        state = STATE_STARTING;

        Librarian.instance().invalidateCountCache();

        AzureusManager.create(this);
        //TransferManager.instance().loadTorrents();

        if (AzureusManager.isCreated()) { // safe move
            AzureusManager.instance().resume();
        }
        
        PeerManager.instance().clear();
        
        PeerManager.instance().start();

        state = STATE_STARTED;
        Log.v(TAG, "Engine started");
    }

    public synchronized void stopServices(boolean disconnected) {
        if (isStopped() || isStopping() || isDisconnected()) {
            return;
        }

        state = STATE_STOPPING;

        AzureusManager.instance().pause();

        PeerManager.instance().clear();
        
        PeerManager.instance().stop();

        state = disconnected ? STATE_DISCONNECTED : STATE_STOPPED;
        Log.v(TAG, "Engine stopped, state: " + state);
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public void notifyDownloadFinished(String displayName, File file) {
        try {
            Context context = getApplicationContext();

            Intent i = new Intent(context, MainActivity.class);

            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION, true);
            i.putExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH, file.getAbsolutePath());

            PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.frostwire_notification, getString(R.string.download_finished), System.currentTimeMillis());
            notification.vibrate = ConfigurationManager.instance().vibrateOnFinishedDownload() ? VENEZUELAN_VIBE : null;
            notification.number = TransferManager.instance().getDownloadsToReview();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.setLatestEventInfo(context, getString(R.string.download_finished), displayName, pi);
            manager.notify(Constants.NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED, notification);
        } catch (Throwable e) {
            Log.e(TAG, "Error creating notification for download finished", e);
        }
    }

    @Override
    public DesktopUploadManager getDesktopUploadManager() {
        return null;
    }

    private void registerPreferencesChangeListener() {
        preferenceListener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(Constants.PREF_KEY_GUI_NICKNAME)) {
                    PeerManager.instance().clear();
                }
            }
        };
        ConfigurationManager.instance().registerOnPreferenceChange(preferenceListener);
    }

    private static long[] buildVenezuelanVibe() {

        long shortVibration = 80;
        long mediumVibration = 100;
        long shortPause = 100;
        long mediumPause = 150;
        long longPause = 180;

        return new long[] { 0, shortVibration, longPause, shortVibration, shortPause, shortVibration, shortPause, shortVibration, mediumPause, mediumVibration };
    }

    public class EngineServiceBinder extends Binder {
        public IEngineService getService() {
            return EngineService.this;
        }
    }
}
