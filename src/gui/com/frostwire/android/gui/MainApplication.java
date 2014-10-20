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

package com.frostwire.android.gui;

import android.app.Application;
import android.content.Context;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ViewConfiguration;
import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.android.util.HttpResponseCache;
import com.frostwire.android.util.ImageLoader;
import com.frostwire.bittorrent.BTContext;
import com.frostwire.bittorrent.BTEngine;
import com.frostwire.logging.Logger;
import com.frostwire.search.CrawlPagedWebSearchPerformer;
import com.frostwire.util.DirectoryUtils;
import com.frostwire.vuze.VuzeConfiguration;
import com.frostwire.vuze.VuzeManager;
import org.gudy.azureus2.core3.util.protocol.AzURLStreamHandlerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gubatron
 * @author aldenml
 */
public class MainApplication extends Application {

    private static final Logger LOG = Logger.getLogger(MainApplication.class);

    @Override
    public void onCreate() {
        super.onCreate();

        ignoreHardwareMenu();

        try {
            HttpResponseCache.install(this);
        } catch (IOException e) {
            LOG.error("Unable to install global http cache", e);
        }

        com.frostwire.android.util.ImageLoader.getInstance(this);
        CrawlPagedWebSearchPerformer.setCache(new DiskCrawlCache(this));
        CrawlPagedWebSearchPerformer.setMagnetDownloader(new LibTorrentMagnetDownloader());

        try {

            //            if (!Librarian.instance().isExternalStorageMounted() || instance != null) {
            //                return;
            //            }

            // important initial setup here
            ConfigurationManager.create(this);

            // important setup at the very beginning
            String azureusPath = SystemUtils.getAzureusDirectory(this).getAbsolutePath();
            String torrentsPath = SystemUtils.getTorrentsDirectory().getAbsolutePath();
            Map<String, String> messages = getVuzeMessages(this);
            VuzeConfiguration conf = new VuzeConfiguration(azureusPath, torrentsPath, messages);
            VuzeManager.setConfiguration(conf);

            setupBTEngine();

            NetworkManager.create(this);
            Librarian.create(this);
            Engine.create(this);

            LocalSearchEngine.create(getDeviceId());//getAndroidId());

            // to alleviate a little if the external storage is not mounted
            if (com.frostwire.android.util.SystemUtils.isPrimaryExternalStorageMounted()) {
                DirectoryUtils.deleteFolderRecursively(SystemUtils.getTempDirectory());
            }

            Librarian.instance().syncMediaStore();
            Librarian.instance().syncApplicationsProvider();
        } catch (Throwable e) {
            String stacktrace = Log.getStackTraceString(e);
            throw new RuntimeException("MainApplication Create exception: " + stacktrace, e);
        }
    }

    private String getDeviceId() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = tm.getDeviceId();

        //probably it's a tablet... Sony's tablet returns null here.
        if (deviceId == null) {
            deviceId = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        }
        return deviceId;
    }

    private Map<String, String> getVuzeMessages(Context context) {
        Map<String, String> msgs = new HashMap<String, String>();

        msgs.put("PeerManager.status.finished", context.getString(R.string.azureus_peer_manager_status_finished));
        msgs.put("PeerManager.status.finishedin", context.getString(R.string.azureus_peer_manager_status_finishedin));
        msgs.put("Formats.units.alot", context.getString(R.string.azureus_formats_units_alot));
        msgs.put("discarded", context.getString(R.string.azureus_discarded));
        msgs.put("ManagerItem.waiting", context.getString(R.string.azureus_manager_item_waiting));
        msgs.put("ManagerItem.initializing", context.getString(R.string.azureus_manager_item_initializing));
        msgs.put("ManagerItem.allocating", context.getString(R.string.azureus_manager_item_allocating));
        msgs.put("ManagerItem.checking", context.getString(R.string.azureus_manager_item_checking));
        msgs.put("ManagerItem.finishing", context.getString(R.string.azureus_manager_item_finishing));
        msgs.put("ManagerItem.ready", context.getString(R.string.azureus_manager_item_ready));
        msgs.put("ManagerItem.downloading", context.getString(R.string.azureus_manager_item_downloading));
        msgs.put("ManagerItem.seeding", context.getString(R.string.azureus_manager_item_seeding));
        msgs.put("ManagerItem.superseeding", context.getString(R.string.azureus_manager_item_superseeding));
        msgs.put("ManagerItem.stopping", context.getString(R.string.azureus_manager_item_stopping));
        msgs.put("ManagerItem.stopped", context.getString(R.string.azureus_manager_item_stopped));
        msgs.put("ManagerItem.paused", context.getString(R.string.azureus_manager_item_paused));
        msgs.put("ManagerItem.queued", context.getString(R.string.azureus_manager_item_queued));
        msgs.put("ManagerItem.error", context.getString(R.string.azureus_manager_item_error));
        msgs.put("ManagerItem.forced", context.getString(R.string.azureus_manager_item_forced));
        msgs.put("GeneralView.yes", context.getString(R.string.azureus_general_view_yes));
        msgs.put("GeneralView.no", context.getString(R.string.azureus_general_view_no));

        return msgs;
    }

    @Override
    public void onLowMemory() {
        ImageLoader.getInstance(this).clear();
        super.onLowMemory();
    }

    private void ignoreHardwareMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field f = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (f != null) {
                f.setAccessible(true);
                f.setBoolean(config, false);
            }
        } catch (Throwable ex) {
            // Ignore
        }
    }

    private void setupBTEngine() {
        // this hack is only due to the remaining vuze TOTorrent code
        URL.setURLStreamHandlerFactory(new AzURLStreamHandlerFactory());

        BTContext ctx = new BTContext();
        ctx.homeDir = SystemUtils.getLibTorrentDirectory(this);
        ctx.torrentsDir = SystemUtils.getTorrentsDirectory();
        ctx.dataDir = SystemUtils.getTorrentDataDirectory();
        ctx.port0 = 0;
        ctx.port1 = 0;
        ctx.iface = "0.0.0.0";
        ctx.optimizeMemory = true;

        BTEngine.ctx = ctx;

        // TODO:BITTORRENT
        BTEngine.getInstance().start();
    }
}
