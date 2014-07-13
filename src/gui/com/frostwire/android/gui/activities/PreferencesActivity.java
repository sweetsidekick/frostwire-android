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

package com.frostwire.android.gui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.View.OnClickListener;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.LocalSearchEngine;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.SearchEngine;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.preference.SimpleActionPreference;
import com.frostwire.android.gui.views.preference.StoragePreference;
import com.frostwire.android.util.StringUtils;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * See {@link ConfigurationManager}
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class PreferencesActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.application_preferences);

        setupConnectButton();
        setupSeedingOptions();
        setupNickname();
        setupClearIndex();
        setupSearchEngines();
        setupUPnPOption();
        setupUXStatsOption();
        
        String action = getIntent().getAction();
        if (action != null && action.equals(Constants.ACTION_SETTINGS_SELECT_STORAGE)) {
            getIntent().setAction(null);
            invokeStoragePreference();
        }
    }

    private void invokeStoragePreference() {
		final StoragePreference storagePreference = (StoragePreference) findPreference(Constants.PREF_KEY_STORAGE_PATH);
		if (storagePreference != null) {
			storagePreference.showDialog(null);
		}
	}

	private void setupSeedingOptions() {
        final CheckBoxPreference preferenceSeeding = (CheckBoxPreference) findPreference(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS);
        final CheckBoxPreference preferenceSeedingWifiOnly = (CheckBoxPreference) findPreference(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY);

        preferenceSeedingWifiOnly.setEnabled(preferenceSeeding.isChecked());

        preferenceSeeding.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newVal = (Boolean) newValue;
                if (!newVal) { // not seeding at all
                    TransferManager.instance().stopSeedingTorrents();
                    UIUtils.showShortMessage(PreferencesActivity.this, R.string.seeding_has_been_turned_off);
                }
                preferenceSeedingWifiOnly.setEnabled(newVal);

                UXStats.instance().log(newVal ? UXAction.SHARING_SEEDING_ENABLED : UXAction.SHARING_SEEDING_DISABLED);

                return true;
            }
        });

        preferenceSeedingWifiOnly.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newVal = (Boolean) newValue;
                if (newVal && !NetworkManager.instance().isDataWIFIUp()) { // not seeding on mobile data
                    TransferManager.instance().stopSeedingTorrents();
                    UIUtils.showShortMessage(PreferencesActivity.this, R.string.seeding_has_been_turned_off);
                }
                return true;
            }
        });
    }

    private void setupNickname() {
        EditTextPreference preference = (EditTextPreference) findPreference(Constants.PREF_KEY_GUI_NICKNAME);
        preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newText = ((String) newValue).trim();
                return !StringUtils.isNullOrEmpty(newText, true);
            }
        });
    }

    private void setupClearIndex() {
        final SimpleActionPreference preference = (SimpleActionPreference) findPreference("frostwire.prefs.internal.clear_index");
        updateIndexSummary(preference);
        preference.setOnActionListener(new OnClickListener() {
            public void onClick(View v) {
                LocalSearchEngine.instance().clearCache();
                UIUtils.showShortMessage(PreferencesActivity.this, R.string.deleted_crawl_cache);
                updateIndexSummary(preference);
            }
        });
    }

    private void setupSearchEngines() {
    	PreferenceScreen category = (PreferenceScreen) findPreference(Constants.PREF_KEY_SEARCH_PREFERENCE_CATEGORY);
        for (SearchEngine engine : SearchEngine.getEngines()) {
            CheckBoxPreference preference = (CheckBoxPreference) findPreference(engine.getPreferenceKey());
            if (!engine.isActive()) {
                category.removePreference(preference);
            }
        }
    }

    private void updateIndexSummary(SimpleActionPreference preference) {
        float size = (((float) LocalSearchEngine.instance().getCacheSize()) / 1024) / 1024;
        preference.setSummary(getString(R.string.crawl_cache_size, size));
    }

    private void updateConnectButton() {
        SimpleActionPreference preference = (SimpleActionPreference) findPreference("frostwire.prefs.internal.connect_disconnect");
        if (Engine.instance().isStarted()) {
            preference.setButtonText(R.string.disconnect);
            preference.setButtonEnabled(true);
        } else if (Engine.instance().isStarting() || Engine.instance().isStopping()) {
            connectButtonImOnIt(preference);
        } else if (Engine.instance().isStopped() || Engine.instance().isDisconnected()) {
            preference.setButtonText(R.string.connect);
            preference.setButtonEnabled(true);
        }
    }

    private void connectButtonImOnIt(SimpleActionPreference preference) {
        preference.setButtonText(R.string.im_on_it);
        preference.setButtonEnabled(false);
    }

    private void setupUPnPOption() {
        final CheckBoxPreference preferenceUPnP = (CheckBoxPreference) findPreference(Constants.PREF_KEY_NETWORK_USE_UPNP);

        preferenceUPnP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newVal = (Boolean) newValue;
                if (newVal) {
                    PeerManager.instance().start();
                } else {
                    PeerManager.instance().stop();
                }
                return true;
            }
        });
    }

    private void setupConnectButton() {
        updateConnectButton();
        SimpleActionPreference preference = (SimpleActionPreference) findPreference("frostwire.prefs.internal.connect_disconnect");

        preference.setOnActionListener(new OnClickListener() {
            public void onClick(View v) {
                if (Engine.instance().isStarted()) {
                    disconnect();
                } else if (Engine.instance().isStopped() || Engine.instance().isDisconnected()) {
                    connect();
                }
            }
        });
    }

    private void setupUXStatsOption() {
        final CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(Constants.PREF_KEY_UXSTATS_ENABLED);

        checkPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newVal = (Boolean) newValue;
                if (!newVal) { // not send ux stats
                    UXStats.instance().setContext(null);
                }
                return true;
            }
        });
    }

    private void connect() {
        final Activity context = this;
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Engine.instance().startServices();
                
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SimpleActionPreference preference = (SimpleActionPreference) findPreference("frostwire.prefs.internal.connect_disconnect");
                        connectButtonImOnIt(preference);
                    }
                });
                
                PeerManager.instance().start();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                UIUtils.showShortMessage(context, R.string.toast_on_connect);
                updateConnectButton();
                if (!(context instanceof MainActivity)) {
                    Intent i = new Intent(context, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(i);
                }
            }
        };

        task.execute();
    }

    private void disconnect() {
        final Context context = this;
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Engine.instance().stopServices(true);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                UIUtils.showShortMessage(context, R.string.toast_on_disconnect);
                updateConnectButton();
                if (!(context instanceof MainActivity)) {
                    Intent i = new Intent(context, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(i);
                }
            }
        };

        task.execute();
    }
}