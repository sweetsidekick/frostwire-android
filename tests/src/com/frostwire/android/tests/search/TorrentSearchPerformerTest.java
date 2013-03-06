/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.android.tests.search;

import java.util.List;

import org.gudy.azureus2.core3.torrent.TOTorrent;

import android.test.ApplicationTestCase;
import android.test.mock.MockApplication;
import android.test.suitebuilder.annotation.MediumTest;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.gui.transfers.AzureusManager;
import com.frostwire.search.SearchResult;
import com.frostwire.search.TorrentSearchPerformer;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class TorrentSearchPerformerTest extends ApplicationTestCase<MockApplication> {

    public TorrentSearchPerformerTest() {
        this(MockApplication.class);
    }

    public TorrentSearchPerformerTest(Class<MockApplication> applicationClass) {
        super(applicationClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ConfigurationManager.create(getApplication());
        AzureusManager.initConfiguration();
    }

    @MediumTest
    public void testDownloadTorrent() {
        downloadTorrent("http://ca.isohunt.com/download/442362661/frostclick.torrent", "http://isohunt.com/torrent_details/442362661/frostclick?tab=summary");
        downloadTorrent("http://www.clearbits.net/get/134-big-buck-bunny-720p.torrent", "http://www.clearbits.net/torrents/134-big-buck-bunny-720p");
    }

    private void downloadTorrent(final String url, final String referrer) {
        TorrentSearchPerformer p = new TorrentSearchPerformer(0, null, 0, 0) {

            @Override
            public void perform() {
                TOTorrent torrent = downloadTorrent(url, referrer);
                assertNotNull("Unable to download torrent: " + url, torrent);
            }

            @Override
            protected String getUrl(int page, String encodedKeywords) {
                return null;
            }

            @Override
            protected List<? extends SearchResult> searchPage(String page) {
                return null;
            }
        };

        p.perform();
    }
}
