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

import java.util.HashMap;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.android.gui.views.ImageLoader;
import com.frostwire.util.DirectoryUtils;
import com.frostwire.vuze.VuzeConfiguration;
import com.frostwire.vuze.VuzeManager;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            
//            if (!Librarian.instance().isExternalStorageMounted() || instance != null) {
//                return;
//            }
            
            // important initial setup here
            ConfigurationManager.create(this);

            // important setup at very begining
            String azureusPath = SystemUtils.getAzureusDirectory().getAbsolutePath();
            String torrentsPath  = SystemUtils.getTorrentsDirectory().getAbsolutePath();
            Map<String, String> messages = getVuzeMessages(this);
            VuzeConfiguration conf = new VuzeConfiguration(azureusPath, torrentsPath, messages);
            VuzeManager.setConfiguration(conf);
            
            NetworkManager.create(this);
            Librarian.create(this);
            Engine.create(this);
            
            LocalSearchEngine.create(getDeviceId());//getAndroidId());

            ImageLoader.createDefaultInstance(this);

            DirectoryUtils.deleteFolderRecursively(SystemUtils.getTempDirectory());

            Librarian.instance().syncMediaStore();
            Librarian.instance().syncApplicationsProvider();
        } catch (Throwable e) {
            String stacktrace = Log.getStackTraceString(e);
            throw new RuntimeException("MainApplication Create exception: " + stacktrace, e);
        }
    }

    private String getAndroidId() {
        return Secure.getString(getContentResolver(), Secure.ANDROID_ID);
    }
    
    private String getDeviceId() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId();
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
}
