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

package com.frostwire.android.gui.adapters;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.adapters.menu.DeleteFileMenuAction;
import com.frostwire.android.gui.adapters.menu.DownloadCheckedMenuAction;
import com.frostwire.android.gui.adapters.menu.DownloadMenuAction;
import com.frostwire.android.gui.adapters.menu.OpenMenuAction;
import com.frostwire.android.gui.adapters.menu.RenameFileMenuAction;
import com.frostwire.android.gui.adapters.menu.SendFileMenuAction;
import com.frostwire.android.gui.adapters.menu.SetAsRingtoneMenuAction;
import com.frostwire.android.gui.adapters.menu.SetAsWallpaperMenuAction;
import com.frostwire.android.gui.adapters.menu.SetSharedStateFileGrainedMenuAction;
import com.frostwire.android.gui.adapters.menu.ToggleFileGrainedSharingMenuAction;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.DownloadTransfer;
import com.frostwire.android.gui.transfers.ExistingDownload;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractListAdapter;
import com.frostwire.android.gui.views.BrowseThumbnailImageButton;
import com.frostwire.android.gui.views.BrowseThumbnailImageButton.OverlayState;
import com.frostwire.android.gui.views.ImageLoader;
import com.frostwire.android.gui.views.ListAdapterFilter;
import com.frostwire.android.gui.views.MenuAction;
import com.frostwire.android.gui.views.MenuAdapter;
import com.frostwire.android.gui.views.MenuBuilder;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * Adapter in control of the List View shown when we're browsing the files of
 * one peer.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class FileListAdapter extends AbstractListAdapter<FileDescriptor> {

    @SuppressWarnings("unused")
    private static final String TAG = "FW.FileListAdapter";

    private final ListView listView;
    private final Peer peer;
    private final boolean local;
    private final byte fileType;
    private final ImageLoader thumbnailLoader;

    private final PadLockClickListener padLockClickListener;
    private final DownloadButtonClickListener downloadButtonClickListener;

    public static final int FILE_LIST_FILTER_SHOW_ALL = 0;
    public static final int FILE_LIST_FILTER_SHOW_SHARED = 1;
    public static final int FILE_LIST_FILTER_SHOW_UNSHARED = 2;

    private FileListFilter fileListFilter;

    public FileListAdapter(ListView listView, List<FileDescriptor> files, Peer peer, boolean local, byte fileType) {
        super(listView.getContext(), getViewItemId(local, fileType), files);
        this.listView = listView;
        
        setShowMenuOnClick(true);

        fileListFilter = new FileListFilter();
        setAdapterFilter(fileListFilter);

        this.peer = peer;
        this.local = local;
        this.fileType = fileType;
        this.thumbnailLoader = ImageLoader.getDefault();

        this.padLockClickListener = new PadLockClickListener();
        this.downloadButtonClickListener = new DownloadButtonClickListener();
    }

    public byte getFileType() {
        return fileType;
    }

    /**
     * @param sharedState FILE_LIST_FILTER_SHOW_ALL, FILE_LIST_FILTER_SHOW_SHARED, FILE_LIST_FILTER_SHOW_UNSHARED
     */
    public void setFileVisibilityBySharedState(int sharedState) {
        fileListFilter.filterBySharedState(sharedState);
    }

    public int getFileVisibilityBySharedState() {
        return fileListFilter.getCurrentSharedStateShown();
    }

    @Override
    protected final void populateView(View view, FileDescriptor file) {
        if (getViewItemId() == R.layout.view_browse_thumbnail_peer_list_item) {
            populateViewThumbnail(view, file);
        } else {
            populateViewPlain(view, file);
        }
    }

    @Override
    protected MenuAdapter getMenuAdapter(View view) {
        Context context = getContext();

        List<MenuAction> items = new ArrayList<MenuAction>();

        FileDescriptor fd = (FileDescriptor) view.getTag();

        List<FileDescriptor> checked = new ArrayList<FileDescriptor>(getChecked());
        int numChecked = checked.size();

        boolean showSingleOptions = showSingleOptions(checked, fd);

        if (local) {
            if (showSingleOptions) {
                items.add(new OpenMenuAction(context, fd.filePath, fd.mime));

                if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS && numChecked <= 1) {
                    items.add(new SendFileMenuAction(context, fd)); //applications cause a force close with GMail
                }

                if (fd.fileType == Constants.FILE_TYPE_RINGTONES && numChecked <= 1) {
                    items.add(new SetAsRingtoneMenuAction(context, fd));
                }

                if (fd.fileType == Constants.FILE_TYPE_PICTURES && numChecked <= 1) {
                    items.add(new SetAsWallpaperMenuAction(context, fd));
                }

                if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS && numChecked <= 1) {
                    items.add(new RenameFileMenuAction(context, this, fd));
                }
            }

            List<FileDescriptor> list = checked;
            if (list.size() == 0) {
                list = Arrays.asList(fd);
            }

            //Share Selected
            items.add(new SetSharedStateFileGrainedMenuAction(context, this, list, true));

            //Unshare Selected
            items.add(new SetSharedStateFileGrainedMenuAction(context, this, list, false));

            //Toogle Shared States
            items.add(new ToggleFileGrainedSharingMenuAction(context, this, list));

            if (fd.fileType != Constants.FILE_TYPE_APPLICATIONS) {
                items.add(new DeleteFileMenuAction(context, this, list));
            }
        } else {
            if (0 < numChecked && numChecked <= Constants.MAX_NUM_DOWNLOAD_CHECKED) {
                items.add(new DownloadCheckedMenuAction(context, this, checked, peer));
            }

            items.add(new DownloadMenuAction(context, this, peer, fd));
        }

        return new MenuAdapter(context, fd.title, items);
    }

    private void localPlay(FileDescriptor fd) {
        if (fd == null) {
            return;
        }

        saveListViewVisiblePosition();
        
        if (fd.mime != null && fd.mime.contains("audio")) {
            if (fd.equals(Engine.instance().getMediaPlayer().getCurrentFD())) {
                Engine.instance().getMediaPlayer().stop();
            } else {
                UIUtils.playEphemeralPlaylist(fd);
                UXStats.instance().log(UXAction.LIBRARY_PLAY_AUDIO_FROM_FILE);
            }
            notifyDataSetChanged();
        } else {
            if (fd.filePath != null && fd.mime != null) {
                UIUtils.openFile(getContext(), fd.filePath, fd.mime);
            }
        }
    }

    public void saveListViewVisiblePosition() {
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        ConfigurationManager.instance().setInt(Constants.BROWSE_PEER_FRAGMENT_LISTVIEW_FIRST_VISIBLE_POSITION + fileType, firstVisiblePosition);
    }
    
    public int getSavedListViewVisiblePosition() {
        //will return 0 if not found.
        return ConfigurationManager.instance().getInt(Constants.BROWSE_PEER_FRAGMENT_LISTVIEW_FIRST_VISIBLE_POSITION  + fileType);
    }

    /**
     * Start a transfer
     */
    private DownloadTransfer startDownload(FileDescriptor fd) {
        DownloadTransfer download = TransferManager.instance().download(peer, fd);
        notifyDataSetChanged();
        return download;
    }

    private void populateViewThumbnail(View view, FileDescriptor fd) {
        BrowseThumbnailImageButton fileThumbnail = findView(view, R.id.view_browse_peer_list_item_file_thumbnail);
        fileThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (local && fileType == Constants.FILE_TYPE_APPLICATIONS) {
            InputStream is = null;

            try {
                thumbnailLoader.displayImage(fd, fileThumbnail, null);
            } catch (Throwable e) {
                fileThumbnail.setImageDrawable(null);
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            if (fd.equals(Engine.instance().getMediaPlayer().getCurrentFD())) {
                fileThumbnail.setOverlayState(OverlayState.STOP);
            } else {
                fileThumbnail.setOverlayState(OverlayState.PLAY);
            }
            thumbnailLoader.displayImage(fd, fileThumbnail, null);
        }

        ImageButton padlock = findView(view, R.id.view_browse_peer_list_item_lock_toggle);

        TextView title = findView(view, R.id.view_browse_peer_list_item_file_title);
        title.setText(fd.title);

        populatePadlockAppearance(fd, padlock, title);

        if (fd.fileType == Constants.FILE_TYPE_AUDIO || fd.fileType == Constants.FILE_TYPE_APPLICATIONS) {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(fd.artist);
        } else {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(R.string.empty_string);
        }

        TextView fileSize = findView(view, R.id.view_browse_peer_list_item_file_size);
        fileSize.setText(UIUtils.getBytesInHuman(fd.fileSize));

        fileThumbnail.setTag(fd);
        fileThumbnail.setOnClickListener(downloadButtonClickListener);
    }

    /**
     * Same factors are considered to show the padlock icon state and color.
     * 
     * When the file is not local and it's been marked for download the text color appears as blue.
     * 
     * @param fd
     * @param padlock
     * @param title
     */
    private void populatePadlockAppearance(FileDescriptor fd, ImageButton padlock, TextView title) {
        if (local) {
            padlock.setVisibility(View.VISIBLE);
            padlock.setTag(fd);
            padlock.setOnClickListener(padLockClickListener);

            if (fd.shared) {
                padlock.setImageResource(R.drawable.browse_peer_padlock_unlocked_icon);
            } else {
                padlock.setImageResource(R.drawable.browse_peer_padlock_locked_icon);
            }
        } else {
            padlock.setVisibility(View.GONE);
        }
    }

    private void populateViewPlain(View view, FileDescriptor fd) {
        ImageButton padlock = findView(view, R.id.view_browse_peer_list_item_lock_toggle);

        TextView title = findView(view, R.id.view_browse_peer_list_item_file_title);
        title.setText(fd.title);

        populatePadlockAppearance(fd, padlock, title);
        populateContainerAction(view);

        if (fd.fileType == Constants.FILE_TYPE_AUDIO || fd.fileType == Constants.FILE_TYPE_APPLICATIONS) {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(fd.artist);
        } else {
            TextView fileExtra = findView(view, R.id.view_browse_peer_list_item_extra_text);
            fileExtra.setText(R.string.empty_string);
        }

        TextView fileSize = findView(view, R.id.view_browse_peer_list_item_file_size);
        fileSize.setText(UIUtils.getBytesInHuman(fd.fileSize));

        BrowseThumbnailImageButton downloadButton = findView(view, R.id.view_browse_peer_list_item_download);

        if (local) {
            if (fd.equals(Engine.instance().getMediaPlayer().getCurrentFD())) {
                downloadButton.setOverlayState(OverlayState.STOP);
            } else {
                downloadButton.setOverlayState(OverlayState.PLAY);
            }
        } else {
            downloadButton.setImageResource(R.drawable.download_icon);
        }

        downloadButton.setTag(fd);
        downloadButton.setOnClickListener(downloadButtonClickListener);
    }

    private void populateContainerAction(View view) {
        ImageButton preview = findView(view, R.id.view_browse_peer_list_item_button_preview);

        if (local) {
            preview.setVisibility(View.GONE);
        } else {
            // just for now
            preview.setVisibility(View.GONE);
        }
    }

    private boolean showSingleOptions(List<FileDescriptor> checked, FileDescriptor fd) {
        if (checked.size() > 1) {
            return false;
        }
        if (checked.size() == 1) {
            return checked.get(0).equals(fd);
        }
        return true;
    }

    private static int getViewItemId(boolean local, byte fileType) {
        if (local && (fileType == Constants.FILE_TYPE_PICTURES || fileType == Constants.FILE_TYPE_VIDEOS || fileType == Constants.FILE_TYPE_APPLICATIONS || fileType == Constants.FILE_TYPE_AUDIO)) {
            return R.layout.view_browse_thumbnail_peer_list_item;
        } else {
            return R.layout.view_browse_peer_list_item;
        }
    }

    private static class FileListFilter implements ListAdapterFilter<FileDescriptor> {

        private int visibleFiles;

        public void filterBySharedState(int state) {
            this.visibleFiles = state;
        }

        public int getCurrentSharedStateShown() {
            return visibleFiles;
        }

        public boolean accept(FileDescriptor obj, CharSequence constraint) {
            if (visibleFiles != FILE_LIST_FILTER_SHOW_ALL && ((obj.shared && visibleFiles == FILE_LIST_FILTER_SHOW_UNSHARED) || (!obj.shared && visibleFiles == FILE_LIST_FILTER_SHOW_SHARED))) {
                return false;
            }

            String keywords = constraint.toString();

            if (keywords == null || keywords.length() == 0) {
                return true;
            }

            keywords = keywords.toLowerCase(Locale.US);

            FileDescriptor fd = obj;

            if (fd.fileType == Constants.FILE_TYPE_AUDIO) {
                return fd.album.trim().toLowerCase(Locale.US).contains(keywords) || fd.artist.trim().toLowerCase(Locale.US).contains(keywords) || fd.title.trim().toLowerCase(Locale.US).contains(keywords) || fd.filePath.trim().toLowerCase(Locale.US).contains(keywords);
            } else {
                return fd.title.trim().toLowerCase(Locale.US).contains(keywords) || fd.filePath.trim().toLowerCase(Locale.US).contains(keywords);
            }
        }
    }

    private final class PadLockClickListener implements OnClickListener {
        public void onClick(View v) {
            FileDescriptor fd = (FileDescriptor) v.getTag();

            if (fd == null) {
                return;
            }

            fd.shared = !fd.shared;

            UXStats.instance().log(fd.shared ? UXAction.WIFI_SHARING_SHARED : UXAction.WIFI_SHARING_UNSHARED);

            notifyDataSetChanged();
            Librarian.instance().updateSharedStates(fileType, Arrays.asList(fd));
        }
    }

    private final class DownloadButtonClickListener implements OnClickListener {
        public void onClick(View v) {
            FileDescriptor fd = (FileDescriptor) v.getTag();

            if (fd == null) {
                return;
            }

            if (local) {
                localPlay(fd);
            } else {

                List<FileDescriptor> list = new ArrayList<FileDescriptor>(getChecked());

                if (list == null || list.size() == 0) {
                    // if no files are selected, they want to download this one.
                    if (!(startDownload(fd) instanceof ExistingDownload)) {
                        UIUtils.showLongMessage(getContext(), R.string.download_added_to_queue);
                        UIUtils.showTransfersOnDownloadStart(getContext());
                    }
                } else {

                    // if many are selected... do they want to download many
                    // or just this one?
                    List<MenuAction> items = new ArrayList<MenuAction>(2);

                    items.add(new DownloadCheckedMenuAction(getContext(), FileListAdapter.this, list, peer));
                    items.add(new DownloadMenuAction(getContext(), FileListAdapter.this, peer, fd));

                    MenuAdapter menuAdapter = new MenuAdapter(getContext(), R.string.wanna_download_question, items);

                    trackDialog(new MenuBuilder(menuAdapter).show());
                }
            }
        }
    }
}