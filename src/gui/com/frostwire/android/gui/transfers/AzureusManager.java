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

package com.frostwire.android.gui.transfers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.CoreRuntimeException;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.util.concurrent.ExecutorsHelper;
import com.frostwire.vuze.VuzeKeys;
import com.frostwire.vuze.VuzeManager;

/**
 * Class to initialize the azureus core.
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class AzureusManager implements VuzeKeys {

    private static final String TAG = "FW.AzureusManager";

    private AzureusCore azureusCore;

    private OnSharedPreferenceChangeListener preferenceListener;

    private static final ExecutorService SAFE_CONFIG_EXECUTOR;

    static {
        SAFE_CONFIG_EXECUTOR = ExecutorsHelper.newFixedSizeThreadPool(1, "Vuze-Save-Config");
    }

    private static AzureusManager instance;

    public synchronized static void create() {
        if (!Librarian.instance().isExternalStorageMounted() || instance != null) {
            return;
        }
        instance = new AzureusManager();
    }

    public static AzureusManager instance() {
        if (instance == null) {
            throw new CoreRuntimeException("AzureusManager not created");
        }
        return instance;
    }

    private AzureusManager() {

        azureusInit();
    }

    public static void revertToDefaultConfiguration() {
        COConfigurationManager.resetToDefaults();
        autoAdjustBittorrentSpeed();
    }

    public static void autoAdjustBittorrentSpeed() {
        if (COConfigurationManager.getBooleanParameter("Auto Adjust Transfer Defaults")) {

            int up_limit_bytes_per_sec = 0;//getEstimatedUploadCapacityBytesPerSec().getBytesPerSec();
            //int down_limit_bytes_per_sec    = 0;//getEstimatedDownloadCapacityBytesPerSec().getBytesPerSec();

            int up_kbs = up_limit_bytes_per_sec / 1024;

            final int[][] settings = {

            { 56, 2, 20, 40 }, // 56 k/bit
                    { 96, 3, 30, 60 }, { 128, 3, 40, 80 }, { 192, 4, 50, 100 }, // currently we don't go lower than this
                    { 256, 4, 60, 200 }, { 512, 5, 70, 300 }, { 1024, 6, 80, 400 }, // 1Mbit
                    { 2 * 1024, 8, 90, 500 }, { 5 * 1024, 10, 100, 600 }, { 10 * 1024, 20, 110, 750 }, // 10Mbit
                    { 20 * 1024, 30, 120, 900 }, { 50 * 1024, 40, 130, 1100 }, { 100 * 1024, 50, 140, 1300 }, { -1, 60, 150, 1500 }, };

            int[] selected = settings[settings.length - 1];

            // note, we start from 3 to avoid over-restricting things when we don't have
            // a reasonable speed estimate

            for (int i = 3; i < settings.length; i++) {

                int[] setting = settings[i];

                int line_kilobit_sec = setting[0];

                // convert to upload kbyte/sec assuming 80% achieved

                int limit = (line_kilobit_sec / 8) * 4 / 5;

                if (up_kbs <= limit) {

                    selected = setting;

                    break;
                }
            }

            int upload_slots = selected[1];
            int connections_torrent = selected[2];
            int connections_global = selected[3];

            if (upload_slots != COConfigurationManager.getIntParameter("Max Uploads")) {
                COConfigurationManager.setParameter("Max Uploads", upload_slots);
                COConfigurationManager.setParameter("Max Uploads Seeding", upload_slots);
            }

            if (connections_torrent != COConfigurationManager.getIntParameter("Max.Peer.Connections.Per.Torrent")) {
                COConfigurationManager.setParameter("Max.Peer.Connections.Per.Torrent", connections_torrent);
                COConfigurationManager.setParameter("Max.Peer.Connections.Per.Torrent.When.Seeding", connections_torrent / 2);
            }

            if (connections_global != COConfigurationManager.getIntParameter("Max.Peer.Connections.Total")) {
                COConfigurationManager.setParameter("Max.Peer.Connections.Total", connections_global);
            }
        }
    }

    private void azureusInit() {
        registerPreferencesChangeListener();
    }

    private void registerPreferencesChangeListener() {
        preferenceListener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(Constants.PREF_KEY_TORRENT_MAX_DOWNLOAD_SPEED)) {
                    setAzureusParameter(MAX_DOWNLOAD_SPEED);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_UPLOAD_SPEED)) {
                    setAzureusParameter(MAX_UPLOAD_SPEED);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_DOWNLOADS)) {
                    setAzureusParameter(MAX_DOWNLOADS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_UPLOADS)) {
                    setAzureusParameter(MAX_UPLOADS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_TOTAL_CONNECTIONS)) {
                    setAzureusParameter(MAX_TOTAL_CONNECTIONS);
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_TORRENT_CONNECTIONS)) {
                    setAzureusParameter(MAX_TORRENT_CONNECTIONS);
                }
            }
        };
        ConfigurationManager.instance().registerOnPreferenceChange(preferenceListener);
    }

    private void setAzureusParameter(String key) {
        COConfigurationManager.setParameter(key, ConfigurationManager.instance().getLong(key));
        asyncSaveConfiguration();
    }

    private static void asyncSaveConfiguration() {
        SAFE_CONFIG_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    COConfigurationManager.save();
                } catch (Throwable t) {
                    //gubatron 03/26/2013
                    //for some reason we're getting a ConcurrentModification exception
                    //even though the treemap is being cloned inside the save() implementation
                    //also, the dalvik vm is killing the app even though the error
                    //occurrs in a thread.
                    //catching the possible exception and logging for now.
                    Log.e(TAG, "Failed to save configuration", t);
                }
            }
        });
    }
}
