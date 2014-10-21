/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

package com.frostwire.vuze;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;

import com.frostwire.logging.Logger;
import com.frostwire.util.OSUtils;

/**
 * Class to initialize the azureus core.
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class VuzeManager {

    private static final Logger LOG = Logger.getLogger(VuzeManager.class);

    private static VuzeConfiguration conf = null;

    private final AtomicBoolean torrentsLoaded;

    private VuzeManager() {
        if (conf == null) {
            throw new IllegalStateException("no config set");
        }

        setupConfiguration();

        this.torrentsLoaded = new AtomicBoolean(false);
    }

    private static class Loader {
        static VuzeManager INSTANCE = new VuzeManager();
    }

    public static VuzeManager getInstance() {
        return Loader.INSTANCE;
    }


    public VuzeDownloadManager find(byte[] hash) {
        return null;
    }

    public void loadTorrents(final boolean stop, final LoadTorrentsListener loadListener, final VuzeDownloadListener downloadListener) {
        if (!torrentsLoaded.compareAndSet(false, true)) {
            //throw new RuntimeException("Load torrents can't be called twice, review the logic");
            return;
        }
    }

    public long getDataReceiveRate() {
        // TODO:BITTORRENT
        return 0;//core.getGlobalManager().getStats().getDataReceiveRate() / 1000;
    }

    public long getDataSendRate() {
        // TODO:BITTORRENT
        return 0;//core.getGlobalManager().getStats().getDataSendRate() / 1000;
    }

    public void pause(boolean disconnected) {
        // TODO:BITTORRENT
//        if (!disconnected) {
//            core.getGlobalManager().pauseDownloads();
//        } else {
//            List<DownloadManager> downloadManagers = core.getGlobalManager().getDownloadManagers();
//            if (downloadManagers != null && downloadManagers.size() > 0) {
//                TorrentUtil.queueTorrents(downloadManagers.toArray());
//            }
//        }
    }

    public void resume() {
        // TODO:BITTORRENT
//        List<DownloadManager> downloadManagers = core.getGlobalManager().getDownloadManagers();
//        if (downloadManagers != null && downloadManagers.size() > 0) {
//            TorrentUtil.resumeTorrents(downloadManagers.toArray());
//        }
    }

    public void setParameter(String key, long value) {
        // TODO:BITTORRENT
//        COConfigurationManager.setParameter(key, value);
//        COConfigurationManager.save();
    }

    public void revertToDefaultConfiguration() {
        // TODO:BITTORRENT
        //COConfigurationManager.resetToDefaults();
        //autoAdjustBittorrentSpeed();
    }

    public static void setConfiguration(VuzeConfiguration conf) {
        VuzeManager.conf = conf;

        // before anything else
        System.setProperty("az.force.noncvs", "1"); // disable debug and development DHT
        System.setProperty("azureus.loadplugins", "0"); // disable third party azureus plugins

        setApplicationPath(conf.getConfigPath());
        SystemProperties.setUserPath(conf.getConfigPath());
    }

    private void setupConfiguration() {
        // TODO:BITTORRENT
        SystemProperties.APPLICATION_NAME = "azureus";

        //COConfigurationManager.setParameter("Auto Adjust Transfer Defaults", false);
        //COConfigurationManager.setParameter("General_sDefaultTorrent_Directory", conf.getTorrentsPath());

        //disableDefaultPlugins();
        //enablePlugins();

        if (OSUtils.isAndroid()) {
            setTimeGranularityMillis(300);

            // TODO:BITTORRENT
//            COConfigurationManager.setParameter("network.tcp.write.select.time", 1000);
//            COConfigurationManager.setParameter("network.tcp.write.select.min.time", 1000);
//            COConfigurationManager.setParameter("network.tcp.read.select.time", 1000);
//            COConfigurationManager.setParameter("network.tcp.read.select.min.time", 1000);
//            COConfigurationManager.setParameter("network.control.write.idle.time", 1000);
//            COConfigurationManager.setParameter("network.control.read.idle.time", 1000);
//
//            COConfigurationManager.setParameter("network.max.simultaneous.connect.attempts", 1);
//
//            COConfigurationManager.setParameter("Enable incremental file creation", Long.valueOf(1));
        }
    }

    private static void setApplicationPath(String path) {
        try {
            Field f = SystemProperties.class.getDeclaredField("app_path");
            f.setAccessible(true);
            f.set(null, path);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to set vuze application path", e);
        }
    }

    private static void setTimeGranularityMillis(long millis) {
        try {
            Field f = SystemTime.class.getDeclaredField("TIME_GRANULARITY_MILLIS");
            f.setAccessible(true);
            f.set(null, millis);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to set vuze SystemTime.TIME_GRANULARITY_MILLIS", e);
        }
    }

    public static interface LoadTorrentsListener {

        public void onLoad(List<VuzeDownloadManager> dms);
    }
}
