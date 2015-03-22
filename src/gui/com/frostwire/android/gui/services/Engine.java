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

package com.frostwire.android.gui.services;

import android.app.Application;
import android.content.*;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import com.frostwire.android.core.CoreRuntimeException;
import com.frostwire.android.core.player.CoreMediaPlayer;
import com.frostwire.android.gui.services.EngineService.EngineServiceBinder;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.frostwire.logging.Logger;
import com.frostwire.util.ByteUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class Engine implements IEngineService {
    private static final Logger LOG = Logger.getLogger(Engine.class);
    private EngineService service;
    private ServiceConnection connection;
    private EngineBroadcastReceiver receiver;
    private Map<String,byte[]> notifiedDownloads;
    private final File notifiedDat;

    private static Engine instance;

    public synchronized static void create(Application context) {
        if (instance != null) {
            return;
        }
        instance = new Engine(context);
    }

    public static Engine instance() {
        if (instance == null) {
            throw new CoreRuntimeException("Engine not created");
        }
        return instance;
    }

    private Engine(Application context) {
        notifiedDat = new File(context.getExternalFilesDir(null),"notified.dat");
        loadNotifiedDownloads();
        startEngineService(context);
    }

    /**
     * loads a dictionary of infohashes that have been already
     * notified from notified.dat. This binary file packs together
     * infohashes 20 bytes at the time.
     *
     * this method goes through the file, 20 bytes at the time and populates
     * a HashMap we can use to query wether or not we should notify the user
     * in constant time.
     *
     * When we have a new infohash, the file conveniently grows by appending the
     * new 20 bytes of the new hash.
     */
    private void loadNotifiedDownloads() {
        notifiedDownloads = new HashMap<String, byte[]>();

        if (!notifiedDat.exists()) {
            try {
                notifiedDat.createNewFile();
            } catch (Throwable e) {
                e.printStackTrace();
                LOG.error("Could not create notified.dat",e);
            }
        } else {
            try {
                FileInputStream fis = new FileInputStream(notifiedDat);
                while (fis.available() > 0) {
                    //each entry on the file is a fixed sha1 hash.
                    byte[] buffer = new byte[20];
                    fis.read(buffer,0,20);
                    notifiedDownloads.put(ByteUtils.encodeHex(buffer).toLowerCase(),buffer);
                }

                IOUtils.closeQuietly(fis);
            } catch (Throwable e) {
                LOG.error("Error reading notified.dat", e);
            }
        }
    }

    @Override
    public CoreMediaPlayer getMediaPlayer() {
        return service != null ? service.getMediaPlayer() : null;
    }

    public byte getState() {
        return service != null ? service.getState() : IEngineService.STATE_INVALID;
    }

    public boolean isStarted() {
        return service != null ? service.isStarted() : false;
    }

    public boolean isStarting() {
        return service != null ? service.isStarting() : false;
    }

    public boolean isStopped() {
        return service != null ? service.isStopped() : false;
    }

    public boolean isStopping() {
        return service != null ? service.isStopping() : false;
    }

    public boolean isDisconnected() {
        return service != null ? service.isDisconnected() : false;
    }

    public void startServices() {
        if (service != null) {
            service.startServices();
        }
    }

    public void stopServices(boolean disconnected) {
        if (service != null) {
            service.stopServices(disconnected);
        }
    }

    public ExecutorService getThreadPool() {
        return EngineService.threadPool;
    }

    public void notifyDownloadFinished(String displayName, File file, String optionalInfoHash) {
        if (service != null) {
            if (optionalInfoHash != null && !optionalInfoHash.equals("0000000000000000000000000000000000000000")) {
                if (!updateNotifiedTorrentDownloads(optionalInfoHash)) {
                    // did not update, we already knew about it. skip notification.
                    return;
                }
            }
            service.notifyDownloadFinished(displayName, file);
        }
    }

    private boolean updateNotifiedTorrentDownloads(String optionalInfoHash) {
        boolean result = false;
        optionalInfoHash = optionalInfoHash.toLowerCase();
        if (notifiedDownloads.containsKey(optionalInfoHash)) {
            LOG.info("Skipping notification on " + optionalInfoHash);
        } else {
            result = appendNewNotifiedInfoHash(optionalInfoHash);
        }
        return result;
    }

    private boolean appendNewNotifiedInfoHash(String infoHash) {
        boolean result = false;
        if (notifiedDownloads != null && infoHash != null && infoHash.length() == 40) {
            byte[] infoHashBytes = ByteUtils.decodeHex(infoHash);

            synchronized (notifiedDat) {
                try {
                    // Another partial download might have just finished writing
                    // this info hash while I was waiting for the file lock.
                    if (!notifiedDownloads.containsKey(infoHash)) {
                        RandomAccessFile raf = new RandomAccessFile(notifiedDat, "rw");
                        raf.seek(notifiedDat.length());
                        raf.write(infoHashBytes, 0, 20);
                        raf.close();

                        // only if we can write to disk, we'll update the map.
                        notifiedDownloads.put(infoHash, infoHashBytes);
                        result = true;
                    }
                } catch (Throwable e) {
                    LOG.error("Could not append infohash to notified.dat", e);
                    result = false;
                }
            }
        }

        return result;
    }

    public void notifyDownloadFinished(String displayName, File file) {
        notifyDownloadFinished(displayName, file, null);
    }

    @Override
    public void shutdown() {
        if (service != null) {
            if (connection != null) {
                try {
                    getApplication().unbindService(connection);
                } catch (IllegalArgumentException e) {
                }
            }

            if (receiver != null) {
                try {
                    getApplication().unregisterReceiver(receiver);
                } catch (IllegalArgumentException e) {
                }
            }

            service.shutdown();
        }
    }

    /**
     * 
     * @param context This must be the application context, otherwise there will be a leak.
     */
    private void startEngineService(final Context context) {
        Intent i = new Intent();
        i.setClass(context, EngineService.class);
        context.startService(i);
        context.bindService(i, connection = new ServiceConnection() {
            public void onServiceDisconnected(ComponentName name) {
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                // avoids: java.lang.ClassCastException: android.os.BinderProxy cannot be cast to com.frostwire.android.gui.services.EngineService$EngineServiceBinder
                if (service instanceof EngineServiceBinder) {
                    Engine.this.service = ((EngineServiceBinder) service).getService();
                    registerStatusReceiver(context);
                }
            }
        }, Context.BIND_AUTO_CREATE);
    }

    private void registerStatusReceiver(Context context) {
        receiver = new EngineBroadcastReceiver();

        IntentFilter wifiFilter = new IntentFilter();

        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);

        IntentFilter fileFilter = new IntentFilter();

        fileFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        fileFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        fileFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        fileFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        fileFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        fileFilter.addAction(Intent.ACTION_MEDIA_NOFS);
        fileFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        fileFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        fileFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        fileFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        fileFilter.addAction(Intent.ACTION_MEDIA_SHARED);
        fileFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        fileFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        fileFilter.addAction(Intent.ACTION_UMS_CONNECTED);
        fileFilter.addAction(Intent.ACTION_UMS_DISCONNECTED);
        fileFilter.addDataScheme("file");

        IntentFilter connectivityFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        IntentFilter audioFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");

        IntentFilter telephonyFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);

        context.registerReceiver(receiver, wifiFilter);
        context.registerReceiver(receiver, fileFilter);
        context.registerReceiver(receiver, connectivityFilter);
        context.registerReceiver(receiver, audioFilter);
        context.registerReceiver(receiver, packageFilter);
        context.registerReceiver(receiver, telephonyFilter);
    }

    @Override
    public Application getApplication() {
        Application r = null;
        if (service!= null) {
            r = service.getApplication();
        }
        return r;
    }
}
