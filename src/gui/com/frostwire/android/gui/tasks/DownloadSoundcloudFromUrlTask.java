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

package com.frostwire.android.gui.tasks;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.ContextTask;
import com.frostwire.search.SearchResult;
import com.frostwire.search.soundcloud.SoundcloudItem;
import com.frostwire.search.soundcloud.SoundcloudPlaylist;
import com.frostwire.search.soundcloud.SoundcloudSearchResult;
import com.frostwire.util.HttpClientFactory;
import com.frostwire.util.JsonUtils;

public final class DownloadSoundcloudFromUrlTask extends ContextTask<List<SoundcloudSearchResult>> {
    private final String soundcloudUrl;
    
    public DownloadSoundcloudFromUrlTask(Context ctx, String soundcloudUrl) {
        super(ctx);
        this.soundcloudUrl = soundcloudUrl;
    }
    
    public static void startDownloads(Context ctx, List<? extends SearchResult> srs) {
        if (srs!=null && !srs.isEmpty()) {
            for (SearchResult sr : srs) {
                StartDownloadTask task = new StartDownloadTask(ctx, sr);
                task.execute();
            }
            UIUtils.showTransfersOnDownloadStart(ctx);
        }
    }

    @Override
    protected void onPostExecute(Context ctx, List<SoundcloudSearchResult> result) {
        if (!result.isEmpty()) {
            startDownloads(ctx, result);
        }
    }

    @Override
    protected List<SoundcloudSearchResult> doInBackground() {
        final List<SoundcloudSearchResult> scResults = new ArrayList<SoundcloudSearchResult>();
        
        //resolve track information using http://api.soundcloud.com/resolve?url=<url>&client_id=b45b1aa10f1ac2941910a7f0d10f8e28
        final String clientId="b45b1aa10f1ac2941910a7f0d10f8e28";
        try {
            final String resolveURL = "http://api.soundcloud.com/resolve.json?url="+soundcloudUrl+"&client_id="+clientId;
            final String json = HttpClientFactory.newInstance().get(resolveURL,10000);

            if (soundcloudUrl.contains("/sets/")) {
                //download a whole playlist
                final SoundcloudPlaylist playlist = JsonUtils.toObject(json, SoundcloudPlaylist.class);
                for (SoundcloudItem scItem : playlist.tracks) {
                    scResults.add(new SoundcloudSearchResult(scItem, clientId));
                }
            } else {
                //download single track
                final SoundcloudItem scItem = JsonUtils.toObject(json, SoundcloudItem.class);
                if (scItem != null) {
                    scResults.add(new SoundcloudSearchResult(scItem, clientId));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return scResults;
    }
}