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

package com.frostwire.android.gui.fragments;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.Finger;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.adapters.FileListAdapter;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractListFragment;
import com.frostwire.android.gui.views.BrowsePeerSearchBarView;
import com.frostwire.android.gui.views.BrowsePeerSearchBarView.OnActionListener;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class BrowsePeerFragment extends AbstractListFragment implements LoaderCallbacks<Object>, MainFragment {

    private static final String TAG = "FW.BrowsePeerFragment";

    private static final int LOADER_FINGER_ID = 0;
    private static final int LOADER_FILES_ID = 1;

    private final BroadcastReceiver broadcastReceiver;

    private RadioButton buttonAudio;
    private RadioButton buttonRingtones;
    private RadioButton buttonVideos;
    private RadioButton buttonPictures;
    private RadioButton buttonApplications;
    private RadioButton buttonDocuments;

    private BrowsePeerSearchBarView filesBar;

    private FileListAdapter adapter;

    private Peer peer;
    private boolean local;
    private Finger finger;

    private View header;

    private OnRefreshSharedListener onRefreshSharedListener;

    public BrowsePeerFragment() {
        super(R.layout.fragment_browse_peer);

        broadcastReceiver = new LocalBroadcastReceiver();
    }

    public Peer getPeer() {
        if (peer == null) {
            loadPeerFromBundleData();
        }

        if (peer == null) {
            loadPeerFromIntentData();
        }

        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
        this.local = peer.isLocalHost();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        if (peer == null) {
            getPeer();
        }

        if (peer == null) { // save move
            getActivity().finish();
            return;
        }
    }

    @Override
    public Loader<Object> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_FINGER_ID) {
            return createLoaderFinger();
        } else if (id == LOADER_FILES_ID) {
            return createLoaderFiles(args.getByte("fileType"));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data == null) {
            Log.w(TAG, "Something wrong, data is null");
            removePeerAndFinish();
            return;
        }

        if (loader.getId() == LOADER_FINGER_ID) {
            boolean firstCheck = finger == null;
            finger = (Finger) data;

            if (firstCheck) {
                checkNoEmptyButton(finger);
            }
        } else if (loader.getId() == LOADER_FILES_ID) {
            updateFiles((Object[]) data);
        }

        updateHeader();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(Constants.ACTION_MEDIA_PLAYER_PLAY));
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(Constants.ACTION_MEDIA_PLAYER_STOPPED));
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(Constants.ACTION_REFRESH_FINGER));

        getLoaderManager().destroyLoader(LOADER_FINGER_ID);
        getLoaderManager().restartLoader(LOADER_FINGER_ID, null, this);

        if (adapter != null) {
            //adapter.notifyDataSetChanged();
            browseFilesButtonClick(adapter.getFileType());
        } else {
            //browseFilesButtonClick(Constants.FILE_TYPE_AUDIO);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void dismissDialogs() {
        super.dismissDialogs();

        if (adapter != null) {
            adapter.dismissDialogs();
        }
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        header = inflater.inflate(R.layout.view_browse_peer_header, null);

        updateHeader();

        return header;
    }

    public OnRefreshSharedListener getOnRefreshSharedListener() {
        return onRefreshSharedListener;
    }

    public void setOnRefreshSharedListener(OnRefreshSharedListener l) {
        this.onRefreshSharedListener = l;
    }

    @Override
    protected void initComponents(View v) {
        buttonApplications = initRadioButton(v, R.id.fragment_browse_peer_radio_applications, Constants.FILE_TYPE_APPLICATIONS);
        buttonDocuments = initRadioButton(v, R.id.fragment_browse_peer_radio_documents, Constants.FILE_TYPE_DOCUMENTS);
        buttonPictures = initRadioButton(v, R.id.fragment_browse_peer_radio_pictures, Constants.FILE_TYPE_PICTURES);
        buttonVideos = initRadioButton(v, R.id.fragment_browse_peer_radio_videos, Constants.FILE_TYPE_VIDEOS);
        buttonRingtones = initRadioButton(v, R.id.fragment_browse_peer_radio_ringtones, Constants.FILE_TYPE_RINGTONES);
        buttonAudio = initRadioButton(v, R.id.fragment_browse_peer_radio_audio, Constants.FILE_TYPE_AUDIO);

        filesBar = findView(v, R.id.fragment_browse_peer_files_bar);
        filesBar.setOnActionListener(new OnActionListener() {
            public void onCheckAll(View v, boolean isChecked) {
                if (adapter != null) {
                    if (isChecked) {
                        adapter.checkAll();
                    } else {
                        adapter.clearChecked();
                    }
                }
            }

            public void onFilter(View v, String str) {
                if (adapter != null) {
                    adapter.getFilter().filter(str);
                }
            }
        });
    }

    protected void onRefreshShared(byte fileType) {
        try {
            if (onRefreshSharedListener != null && finger != null) {
                int numShared = 0;

                switch (fileType) {
                case Constants.FILE_TYPE_APPLICATIONS:
                    numShared = finger.numSharedApplicationFiles;
                    break;
                case Constants.FILE_TYPE_AUDIO:
                    numShared = finger.numSharedAudioFiles;
                    break;
                case Constants.FILE_TYPE_DOCUMENTS:
                    numShared = finger.numSharedDocumentFiles;
                    break;
                case Constants.FILE_TYPE_PICTURES:
                    numShared = finger.numSharedPictureFiles;
                    break;
                case Constants.FILE_TYPE_RINGTONES:
                    numShared = finger.numSharedRingtoneFiles;
                    break;
                case Constants.FILE_TYPE_VIDEOS:
                    numShared = finger.numSharedVideoFiles;
                    break;
                }

                onRefreshSharedListener.onRefresh(this, fileType, numShared);
            }
        } catch (Throwable e) {
            // this catch is mostly due to the mutable nature of finger and onRefreshSharedListener 
            Log.e(TAG, "Error notifying shared refresh", e);
        }
    }

    private void loadPeerFromIntentData() {
        if (peer != null) { // why?
            return;
        }

        Intent intent = getActivity().getIntent();
        if (intent.hasExtra(Constants.EXTRA_PEER_UUID)) {
            String uuid = intent.getStringExtra(Constants.EXTRA_PEER_UUID);

            if (uuid != null) {
                try {
                    peer = PeerManager.instance().findPeerByUUID(uuid);
                    local = peer.isLocalHost();
                } catch (Throwable e) {
                    peer = null; // weird situation reported by a strange bug.
                }
            }
        }
    }

    private void loadPeerFromBundleData() {
        if (peer != null) { // why?
            return;
        }

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(Constants.EXTRA_PEER_UUID)) {
            String uuid = bundle.getString(Constants.EXTRA_PEER_UUID);

            if (uuid != null) {
                try {
                    peer = PeerManager.instance().findPeerByUUID(uuid);
                    local = peer.isLocalHost();
                } catch (Throwable e) {
                    peer = null; // weird situation reported by a strange bug.
                }
            }
        }
    }

    private RadioButton initRadioButton(View v, int viewId, final byte fileType) {
        final RadioButton button = findView(v, viewId);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button.isChecked()) {
                    browseFilesButtonClick(fileType);
                }
            }
        });
        button.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    browseFilesButtonClick(fileType);
                }
            }
        });

        return button;
    }

    private void browseFilesButtonClick(byte fileType) {
        if (adapter != null) {
            adapter.clear();
            //adapter = null;
        }

        filesBar.clearCheckAll();
        filesBar.clearSearch();

        getLoaderManager().destroyLoader(LOADER_FILES_ID);
        Bundle bundle = new Bundle();
        bundle.putByte("fileType", fileType);
        getLoaderManager().restartLoader(LOADER_FILES_ID, bundle, this);

        onRefreshShared(fileType);
    }

    private Loader<Object> createLoaderFinger() {
        AsyncTaskLoader<Object> loader = new AsyncTaskLoader<Object>(getActivity()) {
            @Override
            public Object loadInBackground() {
                try {
                    return peer.finger();
                } catch (Throwable e) {
                    Log.e(TAG, "Error performing finger", e);
                }
                return null;
            }
        };
        loader.forceLoad();
        return loader;
    }

    private Loader<Object> createLoaderFiles(final byte fileType) {
        AsyncTaskLoader<Object> loader = new AsyncTaskLoader<Object>(getActivity()) {
            @Override
            public Object loadInBackground() {
                try {
                    return new Object[] { fileType, peer.browse(fileType) };
                } catch (Throwable e) {
                    Log.e(TAG, "Error performing finger", e);
                }
                return null;
            }
        };
        loader.forceLoad();
        return loader;
    }

    private void updateHeader() {
        if (finger == null) {
            if (peer == null) {
                Log.w(TAG, "Something wrong, finger  and peer are null");
                removePeerAndFinish();
                return;
            } else {
                finger = peer.finger();
            }
        }

        if (header != null) {

            byte fileType = adapter != null ? adapter.getFileType() : Constants.FILE_TYPE_AUDIO;

            int numShared = 0;
            int numTotal = 0;

            switch (fileType) {
            case Constants.FILE_TYPE_APPLICATIONS:
                numShared = finger.numSharedApplicationFiles;
                numTotal = finger.numTotalApplicationFiles;
                break;
            case Constants.FILE_TYPE_AUDIO:
                numShared = finger.numSharedAudioFiles;
                numTotal = finger.numTotalAudioFiles;
                break;
            case Constants.FILE_TYPE_DOCUMENTS:
                numShared = finger.numSharedDocumentFiles;
                numTotal = finger.numTotalDocumentFiles;
                break;
            case Constants.FILE_TYPE_PICTURES:
                numShared = finger.numSharedPictureFiles;
                numTotal = finger.numTotalPictureFiles;
                break;
            case Constants.FILE_TYPE_RINGTONES:
                numShared = finger.numSharedRingtoneFiles;
                numTotal = finger.numTotalRingtoneFiles;
                break;
            case Constants.FILE_TYPE_VIDEOS:
                numShared = finger.numSharedVideoFiles;
                numTotal = finger.numTotalVideoFiles;
                break;
            }

            String fileTypeStr = getString(R.string.my_filetype, UIUtils.getFileTypeAsString(getResources(), fileType));

            TextView title = (TextView) header.findViewById(R.id.view_browse_peer_header_text_title);
            TextView total = (TextView) header.findViewById(R.id.view_browse_peer_header_text_total);
            TextView shared = (TextView) header.findViewById(R.id.view_browse_peer_header_text_total_shared);
            TextView unshared = (TextView) header.findViewById(R.id.view_browse_peer_header_text_total_unshared);

            FileVisibilityFilterListener visibilityFilterListener = new FileVisibilityFilterListener();
            shared.setOnClickListener(visibilityFilterListener);
            unshared.setOnClickListener(visibilityFilterListener);

            title.setText(fileTypeStr);
            total.setText("(" + String.valueOf(numTotal) + ")");
            shared.setText(String.valueOf(numShared));
            unshared.setText(String.valueOf(numTotal - numShared));

            updateFileVisiblityIndicatorsAlpha();
        }

        if (adapter != null) {
            onRefreshShared(adapter.getFileType());
        } else {
            browseFilesButtonClick(Constants.FILE_TYPE_AUDIO);
        }
    }

    private void updateFiles(Object[] data) {
        if (data == null) {
            Log.w(TAG, "Something wrong, data is null");
            removePeerAndFinish();
            return;
        }

        try {
            byte fileType = (Byte) data[0];

            @SuppressWarnings("unchecked")
            List<FileDescriptor> items = (List<FileDescriptor>) data[1];

            adapter = new FileListAdapter(getListView().getContext(), items, peer, local, fileType) {
                protected void onItemChecked(View v, boolean isChecked) {
                    if (!isChecked) {
                        filesBar.clearCheckAll();
                    }
                }
            };
            adapter.setCheckboxesVisibility(true);
            setListAdapter(adapter);
        } catch (Throwable e) {
            Log.e(TAG, "Error updating files in list", e);
        }
    }

    private void checkNoEmptyButton(Finger f) {
        if (f.numSharedAudioFiles > 0) {
            buttonAudio.setChecked(true);
        } else if (f.numSharedVideoFiles > 0) {
            buttonVideos.setChecked(true);
        } else if (f.numSharedPictureFiles > 0) {
            buttonPictures.setChecked(true);
        } else if (f.numSharedDocumentFiles > 0) {
            buttonDocuments.setChecked(true);
        } else if (f.numSharedApplicationFiles > 0) {
            buttonApplications.setChecked(true);
        } else if (f.numSharedRingtoneFiles > 0) {
            buttonRingtones.setChecked(true);
        } else {
            buttonAudio.setChecked(true);
        }
    }

    private void removePeerAndFinish() {
        Activity activity = getActivity();
        if (activity != null) {
            UIUtils.showShortMessage(activity, R.string.is_not_responding, peer.getNickname());
            PeerManager.instance().removePeer(peer);
            activity.finish();
        }
    }

    private final class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_MEDIA_PLAYER_PLAY) || intent.getAction().equals(Constants.ACTION_MEDIA_PLAYER_STOPPED)) {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            } else if (intent.getAction().equals(Constants.ACTION_REFRESH_FINGER)) {
                getLoaderManager().restartLoader(LOADER_FINGER_ID, null, BrowsePeerFragment.this);
            }
        }
    }

    private class FileVisibilityFilterListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "clicked filter");

            adapter.setFileVisibilityBySharedState((adapter.getFileVisibilityBySharedState() + 1) % 3);
            adapter.getFilter().filter(filesBar.getText());

            updateFileVisiblityIndicatorsAlpha();
        }
    }

    public void updateFileVisiblityIndicatorsAlpha() {

        if (adapter == null) {
            return;
        }

        TextView shared = (TextView) header.findViewById(R.id.view_browse_peer_header_text_total_shared);
        TextView unshared = (TextView) header.findViewById(R.id.view_browse_peer_header_text_total_unshared);

        int transparentValue = 128;

        switch (adapter.getFileVisibilityBySharedState()) {
        case FileListAdapter.FILE_LIST_FILTER_SHOW_ALL:
            UIUtils.setTextViewAlpha(shared, 255);
            UIUtils.setTextViewAlpha(unshared, 255);
            break;
        case FileListAdapter.FILE_LIST_FILTER_SHOW_SHARED:
            UIUtils.setTextViewAlpha(shared, 255);
            UIUtils.setTextViewAlpha(unshared, transparentValue);
            break;
        case FileListAdapter.FILE_LIST_FILTER_SHOW_UNSHARED:
            UIUtils.setTextViewAlpha(shared, transparentValue);
            UIUtils.setTextViewAlpha(unshared, 255);
            break;
        }
    }

    public static interface OnRefreshSharedListener {
        public void onRefresh(Fragment f, byte fileType, int numShared);
    }

    public void refreshSelection() {
        if (adapter != null) {
            browseFilesButtonClick(adapter.getFileType());
        }
    }
}
