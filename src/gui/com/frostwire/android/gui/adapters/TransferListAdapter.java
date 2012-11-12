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

package com.frostwire.android.gui.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.MediaType;
import com.frostwire.android.gui.adapters.menu.BrowsePeerMenuAction;
import com.frostwire.android.gui.adapters.menu.CancelMenuAction;
import com.frostwire.android.gui.adapters.menu.OpenMenuAction;
import com.frostwire.android.gui.adapters.menu.PauseDownloadMenuAction;
import com.frostwire.android.gui.adapters.menu.ResumeDownloadMenuAction;
import com.frostwire.android.gui.transfers.BittorrentDownload;
import com.frostwire.android.gui.transfers.BittorrentDownloadItem;
import com.frostwire.android.gui.transfers.DesktopTransfer;
import com.frostwire.android.gui.transfers.DesktopTransferItem;
import com.frostwire.android.gui.transfers.DownloadTransfer;
import com.frostwire.android.gui.transfers.HttpDownload;
import com.frostwire.android.gui.transfers.PeerHttpDownload;
import com.frostwire.android.gui.transfers.PeerHttpUpload;
import com.frostwire.android.gui.transfers.SoundcloudDownload;
import com.frostwire.android.gui.transfers.TorrentFetcherDownload;
import com.frostwire.android.gui.transfers.Transfer;
import com.frostwire.android.gui.transfers.TransferItem;
import com.frostwire.android.gui.transfers.YouTubeDownload;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.MenuAction;
import com.frostwire.android.gui.views.MenuAdapter;
import com.frostwire.android.gui.views.MenuBuilder;
import com.frostwire.android.util.FilenameUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class TransferListAdapter extends BaseExpandableListAdapter {

    private static final String TAG = "FW.TransferListAdapter";

    private final Context context;

    private final OnClickListener viewOnClickListener;
    private final ViewOnLongClickListener viewOnLongClickListener;
    private final ActionOnClickListener actionOnClickListener;
    private final OpenOnClickListener playOnClickListener;

    /** Keep track of all dialogs ever opened so we dismiss when we leave to avoid memleaks */
    private final List<Dialog> dialogs;

    private List<Transfer> list;

    public TransferListAdapter(Context context, List<Transfer> list) {
        this.context = context;

        this.viewOnClickListener = new ViewOnClickListener();
        this.viewOnLongClickListener = new ViewOnLongClickListener();
        this.actionOnClickListener = new ActionOnClickListener();
        this.playOnClickListener = new OpenOnClickListener();

        this.dialogs = new ArrayList<Dialog>();

        this.list = list.equals(Collections.emptyList()) ? new ArrayList<Transfer>() : list;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return list.get(groupPosition).getItems().get(childPosition);
    }

    public TransferItem getChildItem(int groupPosition, int childPosition) {
        return list.get(groupPosition).getItems().get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        TransferItem item = getChildItem(groupPosition, childPosition);

        if (convertView == null) {
            convertView = View.inflate(context, R.layout.view_transfer_item_list_item, null);

            convertView.setOnClickListener(viewOnClickListener);
            convertView.setOnLongClickListener(viewOnLongClickListener);
        }

        try {

            initTouchFeedback(convertView, item);

            populateChildView(convertView, item);

        } catch (Throwable e) {
            Log.e(TAG, "Fatal error getting view: " + e.getMessage());
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return list.get(groupPosition).getItems().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return list.get(groupPosition);
    }

    public Transfer getGroupItem(int groupPosition) {
        return list.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return list.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        Transfer item = getGroupItem(groupPosition);

        if (convertView == null) {
            convertView = View.inflate(context, R.layout.view_transfer_list_item, null);
        }

        try {
            boolean clickable = item.getItems().size() == 0;
            convertView.setOnClickListener(clickable ? viewOnClickListener : null);
            convertView.setOnLongClickListener(clickable ? viewOnLongClickListener : null);

            convertView.setClickable(clickable);
            convertView.setLongClickable(clickable);

            setupGroupIndicator(convertView, isExpanded, item);

            convertView.setTag(item);
            populateGroupView(convertView, item);
        } catch (Throwable e) {
            Log.e(TAG, "Fatal error getting the group view: " + e.getMessage(), e);
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public void updateList(List<Transfer> g) {
        list = g;
        notifyDataSetChanged();
    }

    public void dismissDialogs() {
        for (Dialog dialog : dialogs) {
            try {
                dialog.dismiss();
            } catch (Throwable e) {
                Log.w(TAG, "Error dismissing dialog", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static <TView extends View> TView findView(View view, int id) {
        return (TView) view.findViewById(id);
    }

    protected void populateGroupView(View view, Transfer transfer) {
        if (transfer instanceof BittorrentDownload) {
            populateBittorrentDownload(view, (BittorrentDownload) transfer);
        } else if (transfer instanceof PeerHttpDownload) {
            populatePeerDownload(view, (PeerHttpDownload) transfer);
        } else if (transfer instanceof PeerHttpUpload) {
            populatePeerUpload(view, (PeerHttpUpload) transfer);
        } else if (transfer instanceof HttpDownload) {
            populateHttpDownload(view, (HttpDownload) transfer);
        } else if (transfer instanceof DesktopTransfer) {
            populateDesktopTransfer(view, (DesktopTransfer) transfer);
        } else if (transfer instanceof YouTubeDownload) {
            populateYouTubeDownload(view, (YouTubeDownload) transfer);
        } else if (transfer instanceof SoundcloudDownload) {
            populateSoundcloudDownload(view, (SoundcloudDownload) transfer);
        }
    }

    protected void populateChildView(View view, TransferItem item) {
        if (item instanceof BittorrentDownloadItem) {
            populateBittorrentDownloadItem(view, (BittorrentDownloadItem) item);
        } else if (item instanceof DesktopTransferItem) {
            populateDesktopTransferItem(view, (DesktopTransferItem) item);
        }
    }

    protected MenuAdapter getMenuAdapter(View view) {
        Object tag = view.getTag();

        String title = "";

        List<MenuAction> items = new ArrayList<MenuAction>();

        if (tag instanceof BittorrentDownload) {
            BittorrentDownload download = (BittorrentDownload) tag;
            title = download.getDisplayName();

            //If it's a torrent download with a single file, we should be able to open it.
            if (download.isComplete() && download.getBittorrentItems().size() == 1) {
                BittorrentDownloadItem transferItem = (BittorrentDownloadItem) download.getBittorrentItems().get(0);
                String path = transferItem.getSavePath().getAbsolutePath();
                String mimeType = UIUtils.getMimeType(path);
                items.add(new OpenMenuAction(context, path, mimeType));
            }

            if (!download.isComplete() || ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS)) {
                if (download.isPausable()) {
                    items.add(new PauseDownloadMenuAction(context, download));
                } else if (download.isResumable()) {
                    items.add(new ResumeDownloadMenuAction(context, download));
                }
            }

            items.add(new CancelMenuAction(context, download, !download.isComplete()));
        } else if (tag instanceof DownloadTransfer) {
            DownloadTransfer download = (DownloadTransfer) tag;
            title = download.getDisplayName();

            boolean openMenu = false;
            openMenu |= download.isComplete() && (tag instanceof HttpDownload || tag instanceof YouTubeDownload || tag instanceof SoundcloudDownload);
            openMenu |= download.isComplete() && tag instanceof DesktopTransfer && ((DesktopTransfer) tag).getItems().size() == 0;

            if (openMenu) {
                items.add(new OpenMenuAction(context, download.getDisplayName(), download.getSavePath().getAbsolutePath(), extractMime(download)));
            } else {
                items.add(new CancelMenuAction(context, download, true));
            }

            if (download instanceof PeerHttpDownload) {
                PeerHttpDownload pdownload = (PeerHttpDownload) download;
                items.add(new BrowsePeerMenuAction(context, pdownload.getPeer()));
            }
        } else if (tag instanceof PeerHttpUpload) {
            PeerHttpUpload upload = (PeerHttpUpload) tag;
            title = upload.getDisplayName();

            items.add(new CancelMenuAction(context, upload, false));
        }

        return items.size() > 0 ? new MenuAdapter(context, title, items) : null;
    }

    protected String extractMime(DownloadTransfer download) {
        if (download instanceof PeerHttpDownload) {
            return ((PeerHttpDownload) download).getFD().mime;
        } else {
            return UIUtils.getMimeType(download.getSavePath().getAbsolutePath());
        }
    }

    protected Dialog trackDialog(Dialog dialog) {
        dialogs.add(dialog);
        return dialog;
    }

    private void setupGroupIndicator(View view, boolean expanded, Transfer item) {
        ImageView groupIndicator = findView(view, R.id.view_transfer_list_item_group_indicator);

        if (groupIndicator != null) {
            if (item.getItems().size() <= 1) {
                //show the file type for the only file there is
                String extension = null;
                String path = null;

                if (item instanceof BittorrentDownload) {
                    BittorrentDownload bItem = (BittorrentDownload) item;
                    if (bItem.getBittorrentItems().size() > 0) {
                        BittorrentDownloadItem transferItem = bItem.getBittorrentItems().get(0);
                        path = transferItem.getSavePath().getAbsolutePath();
                        extension = FilenameUtils.getExtension(path);
                    }
                } else if (item instanceof DownloadTransfer) {
                    DownloadTransfer transferItem = (DownloadTransfer) item;
                    path = transferItem.getSavePath().getAbsolutePath();
                    extension = FilenameUtils.getExtension(path);
                } else if (item instanceof PeerHttpUpload) {
                    PeerHttpUpload transferItem = (PeerHttpUpload) item;
                    path = transferItem.getFD().filePath;
                    extension = FilenameUtils.getExtension(path);
                }

                if (extension != null && extension.equals("apk")) {
                    try {
                        //Apk apk = new Apk(context,path);

                        //TODO: Get the APK Icon so we can show the APK icon on the transfer manager once
                        //it's finished downloading, or as it's uploading to another peer.
                        //apk.getDrawable(id);

                        //in the meantime, just hardcode it
                        groupIndicator.setImageResource(R.drawable.browse_peer_application_icon_selector_off);
                    } catch (Throwable e) {
                        groupIndicator.setImageResource(R.drawable.browse_peer_application_icon_selector_off);
                    }
                } else {
                    groupIndicator.setImageResource(getFileTypeIconId(extension));
                }
            } else {
                groupIndicator.setImageResource(expanded ? R.drawable.transfer_menuitem_minus : R.drawable.transfer_menuitem_plus);
            }
        }
    }

    private void initTouchFeedback(View v, TransferItem item) {
        v.setOnClickListener(viewOnClickListener);
        v.setOnLongClickListener(viewOnLongClickListener);
        v.setTag(item);

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = vg.getChildAt(i);
                initTouchFeedback(child, item);
            }
        }
    }

    private void populateBittorrentDownload(View view, BittorrentDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);

        seeds.setText(context.getString(R.string.seeds_n, download.getSeeds()));
        peers.setText(context.getString(R.string.peers_n, download.getPeers()));

        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());

        if (download instanceof TorrentFetcherDownload) {
            if (((TorrentFetcherDownload) download).getDelegate() != null) {
                status.setText(download.getStatus());
            } else {
                status.setText(Integer.valueOf(download.getStatus()));
            }
        } else {
            status.setText(download.getStatus());
        }

        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(actionOnClickListener);
    }

    private void populatePeerDownload(View view, PeerHttpDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());
        status.setText(Integer.valueOf(download.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(actionOnClickListener);
    }

    private void populatePeerUpload(View view, PeerHttpUpload upload) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(upload.getDisplayName());
        progress.setProgress(upload.getProgress());
        status.setText(Integer.valueOf(upload.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(upload.getUploadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(upload.getSize()));

        buttonAction.setTag(upload);
        buttonAction.setOnClickListener(actionOnClickListener);
    }

    private void populateHttpDownload(View view, HttpDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());
        status.setText(Integer.valueOf(download.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(actionOnClickListener);
    }

    private void populateDesktopTransfer(View view, DesktopTransfer transfer) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(transfer.getDisplayName());
        progress.setProgress(transfer.getProgress());
        status.setText(Integer.valueOf(transfer.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(transfer.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(transfer.getSize()));

        buttonAction.setTag(transfer);
        buttonAction.setOnClickListener(actionOnClickListener);
    }

    private void populateBittorrentDownloadItem(View view, BittorrentDownloadItem item) {
        ImageView icon = findView(view, R.id.view_transfer_item_list_item_icon);
        TextView title = findView(view, R.id.view_transfer_item_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_item_list_item_progress);
        TextView size = findView(view, R.id.view_transfer_item_list_item_size);
        ImageButton buttonPlay = findView(view, R.id.view_transfer_item_list_item_button_play);

        icon.setImageResource(getFileTypeIconId(FilenameUtils.getExtension(item.getSavePath().getAbsolutePath())));
        title.setText(item.getDisplayName());
        progress.setProgress(item.getProgress());
        size.setText(UIUtils.getBytesInHuman(item.getSize()));

        buttonPlay.setTag(item);
        buttonPlay.setVisibility(item.isComplete() ? View.VISIBLE : View.GONE);
        buttonPlay.setOnClickListener(playOnClickListener);
    }

    private void populateDesktopTransferItem(View view, DesktopTransferItem item) {
        ImageView icon = findView(view, R.id.view_transfer_item_list_item_icon);
        TextView title = findView(view, R.id.view_transfer_item_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_item_list_item_progress);
        TextView size = findView(view, R.id.view_transfer_item_list_item_size);
        ImageButton buttonPlay = findView(view, R.id.view_transfer_item_list_item_button_play);

        icon.setImageResource(getFileTypeIconId(FilenameUtils.getExtension(item.getSavePath().getAbsolutePath())));
        title.setText(item.getDisplayName());
        progress.setProgress(item.getProgress());
        size.setText(UIUtils.getBytesInHuman(item.getSize()));

        buttonPlay.setTag(item);
        buttonPlay.setVisibility(item.isComplete() ? View.VISIBLE : View.GONE);
        buttonPlay.setOnClickListener(playOnClickListener);
    }

    private void populateYouTubeDownload(View view, YouTubeDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());
        status.setText(Integer.valueOf(download.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(actionOnClickListener);
    }

    private void populateSoundcloudDownload(View view, SoundcloudDownload download) {
        TextView title = findView(view, R.id.view_transfer_list_item_title);
        ProgressBar progress = findView(view, R.id.view_transfer_list_item_progress);
        TextView status = findView(view, R.id.view_transfer_list_item_status);
        TextView speed = findView(view, R.id.view_transfer_list_item_speed);
        TextView size = findView(view, R.id.view_transfer_list_item_size);
        TextView seeds = findView(view, R.id.view_transfer_list_item_seeds);
        TextView peers = findView(view, R.id.view_transfer_list_item_peers);
        ImageView buttonAction = findView(view, R.id.view_transfer_list_item_button_action);

        seeds.setText("");
        peers.setText("");
        title.setText(download.getDisplayName());
        progress.setProgress(download.getProgress());
        status.setText(Integer.valueOf(download.getStatus()));
        speed.setText(UIUtils.getBytesInHuman(download.getDownloadSpeed()) + "/s");
        size.setText(UIUtils.getBytesInHuman(download.getSize()));

        buttonAction.setTag(download);
        buttonAction.setOnClickListener(actionOnClickListener);
    }

    private static int getFileTypeIconId(String ext) {
        MediaType mt = MediaType.getMediaTypeForExtension(ext);
        if (mt == null) {
            return R.drawable.question_mark;
        }
        if (mt.equals(MediaType.getApplicationsMediaType())) {
            return R.drawable.browse_peer_application_icon_selector_off;
        } else if (mt.equals(MediaType.getAudioMediaType())) {
            return R.drawable.browse_peer_audio_icon_selector_off;
        } else if (mt.equals(MediaType.getDocumentMediaType())) {
            return R.drawable.browse_peer_document_icon_selector_off;
        } else if (mt.equals(MediaType.getImageMediaType())) {
            return R.drawable.browse_peer_picture_icon_selector_off;
        } else if (mt.equals(MediaType.getVideoMediaType())) {
            return R.drawable.browse_peer_video_icon_selector_off;
        } else if (mt.equals(MediaType.getTorrentMediaType())) {
            return R.drawable.browse_peer_torrent_icon_selector_off;
        } else {
            return R.drawable.question_mark;
        }
    }

    private final class ViewOnClickListener implements OnClickListener {
        public void onClick(View v) {
            try {
                MenuAdapter adapter = getMenuAdapter(v);
                if (adapter != null) {
                    trackDialog(new MenuBuilder(adapter).show());
                    return;
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to create the menu", e);
            }
        }
    }

    private final class ViewOnLongClickListener implements OnLongClickListener {
        public boolean onLongClick(View v) {
            try {
                MenuAdapter adapter = getMenuAdapter(v);
                if (adapter != null) {
                    trackDialog(new MenuBuilder(adapter).show());
                    return true;
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to create the menu");
            }
            return false;
        }
    }

    private final class ActionOnClickListener implements OnClickListener {
        public void onClick(View v) {
            final Transfer transfer = (Transfer) v.getTag();

            if (transfer instanceof BittorrentDownload) {
                viewOnClickListener.onClick(v);
            } else {
                trackDialog(UIUtils.showYesNoDialog(context, R.string.yes_no_cancel_transfer_question, R.string.cancel_transfer, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        transfer.cancel();
                    }
                }));
            }
        }
    }

    private final class OpenOnClickListener implements OnClickListener {
        public void onClick(View v) {
            TransferItem item = (TransferItem) v.getTag();

            boolean canOpen = false;
            canOpen |= item.isComplete() && item instanceof BittorrentDownloadItem;
            canOpen |= item.isComplete() && item instanceof DesktopTransferItem;

            if (canOpen) {
                File savePath = null;
                if (item instanceof BittorrentDownloadItem) {
                    savePath = ((BittorrentDownloadItem) item).getSavePath();
                } else if (item instanceof DesktopTransferItem) {
                    savePath = ((DesktopTransferItem) item).getSavePath();
                }

                if (savePath != null) {
                    UIUtils.openFile(context, savePath);
                }
            }
        }
    }
}
