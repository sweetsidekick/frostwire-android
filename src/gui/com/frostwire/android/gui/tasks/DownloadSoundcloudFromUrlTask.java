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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.dialogs.ConfirmListDialog;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractListAdapter;
import com.frostwire.android.gui.views.ContextTask;
import com.frostwire.android.util.ImageLoader;
import com.frostwire.search.SearchResult;
import com.frostwire.search.soundcloud.SoundcloudItem;
import com.frostwire.search.soundcloud.SoundcloudPlaylist;
import com.frostwire.search.soundcloud.SoundcloudSearchResult;
import com.frostwire.util.HttpClientFactory;
import com.frostwire.util.JsonUtils;
import com.frostwire.util.Ref;
import com.frostwire.util.StringUtils;

/*
 * @author aldenml
 * @author gubatron
 */
public final class DownloadSoundcloudFromUrlTask extends ContextTask<List<SoundcloudSearchResult>> {
    private final String soundcloudUrl;
    private WeakReference<ConfirmSoundcloudDownloadDialog> dlgRef;
    
    public DownloadSoundcloudFromUrlTask(Context ctx, String soundcloudUrl) {
        super(ctx);
        this.soundcloudUrl = soundcloudUrl;
    }
    
    private ConfirmSoundcloudDownloadDialog createConfirmListDialog(Context ctx, List<SoundcloudSearchResult> results) {
        String title = ctx.getString(R.string.confirm_download);
        String whatToDownload = ctx.getString((results.size() > 1) ? R.string.playlist : R.string.track); 
        String totalSize = UIUtils.getBytesInHuman(getTotalSize(results));
        String text = ctx.getString(R.string.are_you_sure_you_want_to_download_the_following, whatToDownload, totalSize);
        
        //ConfirmListDialog
        ConfirmSoundcloudDownloadDialog dlg = ConfirmSoundcloudDownloadDialog.newInstance(title, text, results, new OnStartDownloadsClickListener(ctx, results));
        return dlg;
    }
    
    private long getTotalSize(List<SoundcloudSearchResult> results) {
        long totalSizeInBytes = 0;
        for (SoundcloudSearchResult sr : results) {
            totalSizeInBytes += sr.getSize();
        }
        return totalSizeInBytes;
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
    protected void onPostExecute(Context ctx, List<SoundcloudSearchResult> results) {
        if (!results.isEmpty()) {
            MainActivity activity = (MainActivity) ctx;
            ConfirmSoundcloudDownloadDialog dlg = createConfirmListDialog(ctx, results);
            dlgRef = new WeakReference<ConfirmSoundcloudDownloadDialog>(dlg);
            dlg.show(activity.getFragmentManager());
        }
    }
    
    public void dismissDialog() {
        if (Ref.alive(dlgRef)) {
            dlgRef.get().dismiss();
        }
    }

    @Override
    protected List<SoundcloudSearchResult> doInBackground() {
        final List<SoundcloudSearchResult> scResults = new ArrayList<SoundcloudSearchResult>();
        
        //MOCK TEST FOR WHEN PHONE HAS NO INTERNET ACCESS.
        /*
        if (true) {
        	SoundcloudItem sItem = new SoundcloudItem();
        	for (int i=0; i < 10; i++) {
            	sItem.artwork_url="http://dummy.com";
            	sItem.created_at="2014-03-22";
            	sItem.download_url="http://url";
            	sItem.duration=334443;
            	sItem.original_content_size=(i%2==0) ? 1122332: 16200234;
            	sItem.title= (i % 2 == 0) ? "Dummy Title that is a bit longer than usual and perhaps might not fit" : "another one short";
            	sItem.uri="http://uri.com";

        		sItem.id=i;
        	    scResults.add(new SoundcloudSearchResult(sItem,"someClient","someAppId"));
        	}
        	return scResults;
        }*/
        
        //resolve track information using http://api.soundcloud.com/resolve?url=<url>&client_id=b45b1aa10f1ac2941910a7f0d10f8e28
        final String clientId="b45b1aa10f1ac2941910a7f0d10f8e28";
        final String appVersion="dd9d3970";
        try {
            String url = soundcloudUrl;
            if (soundcloudUrl.contains("?in=")) {
                url = soundcloudUrl.substring(0, url.indexOf("?in="));
            }

            final String resolveURL = "http://api.soundcloud.com/resolve.json?url="+url+"&client_id="+clientId+"&app_version="+appVersion;;
            final String json = HttpClientFactory.newInstance().get(resolveURL,10000);

            if (soundcloudUrl.contains("/sets/")) {
                //download a whole playlist
                final SoundcloudPlaylist playlist = JsonUtils.toObject(json, SoundcloudPlaylist.class);
                if (playlist.tracks != null) {
                    for (SoundcloudItem scItem : playlist.tracks) {
                        scResults.add(new SoundcloudSearchResult(scItem, clientId, appVersion));
                    }
                }
            } else {
                //download single track
                final SoundcloudItem scItem = JsonUtils.toObject(json, SoundcloudItem.class);
                if (scItem != null) {
                    scResults.add(new SoundcloudSearchResult(scItem, clientId, appVersion));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return scResults;
    }
    
    private static class SimpleSoundcloudSearchResultAdapter extends AbstractListAdapter<SoundcloudSearchResult> {

        public SimpleSoundcloudSearchResultAdapter(Context context, List<SoundcloudSearchResult> list) {
            super(context, R.layout.list_item_track_confirmation_dialog, list);
        }

        @Override
        protected void populateView(View view, SoundcloudSearchResult sr) {
            TextView trackTitle = findView(view, R.id.list_item_track_confirmation_dialog_track_title);
            trackTitle.setText(sr.getDisplayName());
            
            TextView trackSizeInHuman = findView(view, R.id.list_item_track_confirmation_dialog_file_size_in_human);
            trackSizeInHuman.setText(UIUtils.getBytesInHuman(sr.getSize()));
            
            if (!StringUtils.isNullOrEmpty(sr.getThumbnailUrl())) {
                ImageView imageView = findView(view, R.id.list_item_track_confirmation_dialog_art);
                ImageLoader.getInstance(getContext()).load(Uri.parse(sr.getThumbnailUrl()), imageView);
            }
        }
    }
    
    private static class OnStartDownloadsClickListener implements View.OnClickListener {
        private final WeakReference<Context> ctxRef;
        private final WeakReference<List<SoundcloudSearchResult>> resultsRef;
        private WeakReference<ConfirmSoundcloudDownloadDialog> dlgRef;
        
        public OnStartDownloadsClickListener(Context ctx, List<SoundcloudSearchResult> results) {
            ctxRef = new WeakReference<Context>(ctx);
            resultsRef = new WeakReference<List<SoundcloudSearchResult>>(results);
        }
        
        public void setDialog(ConfirmSoundcloudDownloadDialog dlg){
            dlgRef = new WeakReference<ConfirmSoundcloudDownloadDialog>(dlg);
        }
        
        @Override
        public void onClick(View v) {
            if (Ref.alive(ctxRef)&& Ref.alive(resultsRef)) {
            	//TODO: gotta figure out how the parent dialog class
            	//will interact with the adapter (in a generic way)
            	//to filter out the List<T> when we have checkboxes.
                startDownloads(ctxRef.get(), resultsRef.get());
                
                if (Ref.alive(dlgRef)){
                    dlgRef.get().dismiss();
                }
            }
        }
    }

    private static class SoundcloudSearchResultList {
        List<SoundcloudSearchResult> listData;
    }
    
    public static class ConfirmSoundcloudDownloadDialog extends ConfirmListDialog<SimpleSoundcloudSearchResultAdapter,SoundcloudSearchResult> {
        public ConfirmSoundcloudDownloadDialog() {
            super();
        }
        
        public static ConfirmSoundcloudDownloadDialog newInstance(String dialogTitle, String dialogText, List<SoundcloudSearchResult> listData, OnStartDownloadsClickListener onYesListener) {
            ConfirmSoundcloudDownloadDialog dlg = new ConfirmSoundcloudDownloadDialog();
            SoundcloudSearchResultList srList = new SoundcloudSearchResultList();
            srList.listData = listData;
            dlg.prepareArguments(dialogTitle, dialogText, JsonUtils.toJson(srList));
            dlg.setOnYesListener(onYesListener);
            onYesListener.setDialog(dlg);
            return dlg;
        }

        @Override
        protected List<SoundcloudSearchResult> initListAdapter(final ListView listView, String listDataInJSON) {
            SoundcloudSearchResultList srList = JsonUtils.toObject(listDataInJSON, SoundcloudSearchResultList.class);
            listView.setAdapter(new SimpleSoundcloudSearchResultAdapter(getActivity(), srList.listData));
            return srList.listData;
        }

        @Override
        protected OnStartDownloadsClickListener createOnYesListener(List<SoundcloudSearchResult> listData) {            
        	return new OnStartDownloadsClickListener(getActivity(), listData);
        }
    }
}