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

package com.frostwire.android.gui.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.NetworkManager;
import com.frostwire.android.gui.adapters.TransferListAdapter;
import com.frostwire.android.gui.dialogs.MenuDialog;
import com.frostwire.android.gui.dialogs.MenuDialog.MenuItem;
import com.frostwire.android.gui.transfers.BittorrentDownload;
import com.frostwire.android.gui.transfers.HttpDownload;
import com.frostwire.android.gui.transfers.SoundcloudDownload;
import com.frostwire.android.gui.transfers.Transfer;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.transfers.YouTubeDownload;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractDialog.OnDialogClickListener;
import com.frostwire.android.gui.views.AbstractFragment;
import com.frostwire.android.gui.views.ClearableEditTextView;
import com.frostwire.android.gui.views.ClearableEditTextView.OnActionListener;
import com.frostwire.android.gui.views.ClickAdapter;
import com.frostwire.android.gui.views.TimerObserver;
import com.frostwire.android.gui.views.TimerService;
import com.frostwire.android.gui.views.TimerSubscription;
import com.frostwire.logging.Logger;
import com.frostwire.util.Ref;
import com.frostwire.util.StringUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class TransfersFragment extends AbstractFragment implements TimerObserver, MainFragment, OnDialogClickListener {
    private static final Logger LOG = Logger.getLogger(TransfersFragment.class);
    private static final String SELECTED_STATUS_STATE_KEY = "selected_status";

    private final Comparator<Transfer> transferComparator;

    private final ButtonAddTransferListener buttonAddTransferListener;
    private final ButtonMenuListener buttonMenuListener;
    

    private Button buttonSelectAll;
    private Button buttonSelectDownloading;
    private Button buttonSelectCompleted;
    private ExpandableListView list;
    private TextView textDownloads;
    private TextView textUploads;
    private ClearableEditTextView addTransferUrlTextView;

    private TransferListAdapter adapter;

    private TransferStatus selectedStatus;

    private TimerSubscription subscription;

    public TransfersFragment() {
        super(R.layout.fragment_transfers);

        this.transferComparator = new TransferComparator();
        this.buttonAddTransferListener = new ButtonAddTransferListener(this);
        this.buttonMenuListener = new ButtonMenuListener(this);

        selectedStatus = TransferStatus.ALL;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        UIUtils.initSupportFrostWire(getActivity(), R.id.activity_mediaplayer_donations_view_placeholder);

        if (savedInstanceState != null) {
            selectedStatus = TransferStatus.valueOf(savedInstanceState.getString(SELECTED_STATUS_STATE_KEY, TransferStatus.ALL.name()));
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subscription = TimerService.subscribe(this, 2);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        subscription.unsubscribe();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SELECTED_STATUS_STATE_KEY, selectedStatus.name());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (adapter != null) {
            adapter.dismissDialogs();
        }
    }

    @Override
    public void onTime() {
        if (adapter != null) {
            List<Transfer> transfers = filter(TransferManager.instance().getTransfers(), selectedStatus);
            Collections.sort(transfers, transferComparator);
            adapter.updateList(transfers);
        } else if (this.getActivity() != null) {
            setupAdapter();
        }

        //  format strings
        String sDown = UIUtils.rate2speed(TransferManager.instance().getDownloadsBandwidth());
        String sUp = UIUtils.rate2speed(TransferManager.instance().getUploadsBandwidth());

        // number of uploads (seeding) and downloads
        int downloads = TransferManager.instance().getActiveDownloads();
        int uploads = TransferManager.instance().getActiveUploads();

        textDownloads.setText(downloads + " @ " + sDown);
        textUploads.setText(uploads + " @ " + sUp);
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);

        View header = inflater.inflate(R.layout.view_transfers_header, null);

        TextView text = (TextView) header.findViewById(R.id.view_transfers_header_text_title);
        text.setText(R.string.transfers);

        ImageButton buttonMenu = (ImageButton) header.findViewById(R.id.view_transfers_header_button_menu);
        buttonMenu.setOnClickListener(buttonMenuListener);
        
        ImageButton buttonAddTransfer = (ImageButton) header.findViewById(R.id.view_transfers_header_button_add_transfer);
        buttonAddTransfer.setOnClickListener(buttonAddTransferListener);

        return header;
    }
    
    public void selectStatusTab(TransferStatus status) {
        selectedStatus = status;
        switch (selectedStatus) {
        case ALL:
            buttonSelectAll.performClick();
            break;
        case DOWNLOADING:
            buttonSelectDownloading.performClick();
            break;
        case COMPLETED:
            buttonSelectCompleted.performClick();
            break;
        }
    }

    @Override
    protected void initComponents(View v) {
        buttonSelectAll = findView(v, R.id.fragment_transfers_button_select_all);
        buttonSelectAll.setOnClickListener(new ButtonTabListener(this, TransferStatus.ALL));

        buttonSelectDownloading = findView(v, R.id.fragment_transfers_button_select_downloading);
        buttonSelectDownloading.setOnClickListener(new ButtonTabListener(this, TransferStatus.DOWNLOADING));

        buttonSelectCompleted = findView(v, R.id.fragment_transfers_button_select_completed);
        buttonSelectCompleted.setOnClickListener(new ButtonTabListener(this, TransferStatus.COMPLETED));

        list = findView(v, R.id.fragment_transfers_list);

        textDownloads = findView(v, R.id.fragment_transfers_text_downloads);
        textUploads = findView(v, R.id.fragment_transfers_text_uploads);
        
        addTransferUrlTextView = findView(v, R.id.fragment_transfers_add_transfer_text_input);
        addTransferUrlTextView.replaceSearchIconDrawable(R.drawable.clearable_edittext_add_icon);
        addTransferUrlTextView.setFocusable(true);
        addTransferUrlTextView.setFocusableInTouchMode(true);
        addTransferUrlTextView.setOnKeyListener(new AddTransferTextListener(this));
    }

    private void setupAdapter() {
        List<Transfer> transfers = filter(TransferManager.instance().getTransfers(), selectedStatus);
        Collections.sort(transfers, transferComparator);
        adapter = new TransferListAdapter(TransfersFragment.this.getActivity(), transfers);
        list.setAdapter(adapter);
    }

    private List<Transfer> filter(List<Transfer> transfers, TransferStatus status) {
        Iterator<Transfer> it;

        switch (status) { // replace this filter by a more functional style
        case DOWNLOADING:
            it = transfers.iterator();
            while (it.hasNext()) {
                if (it.next().isComplete()) {
                    it.remove();
                }
            }
            return transfers;
        case COMPLETED:
            it = transfers.iterator();
            while (it.hasNext()) {
                if (!it.next().isComplete()) {
                    it.remove();
                }
            }
            return transfers;
        default:
            return transfers;
        }
    }

    private static final String TRANSFERS_DIALOG_ID = "transfers_dialog";

    private static final int CLEAR_MENU_DIALOG_ID = 0;
    private static final int PAUSE_MENU_DIALOG_ID = 1;
    private static final int RESUME_MENU_DIALOG_ID = 2;


    @Override
    public void onDialogClick(String tag, int which) {
        if (tag.equals(TRANSFERS_DIALOG_ID)) {
            switch (which) {
            case CLEAR_MENU_DIALOG_ID:
                TransferManager.instance().clearComplete();
                break;
            case PAUSE_MENU_DIALOG_ID:
                TransferManager.instance().stopHttpTransfers();
                TransferManager.instance().pauseTorrents();
                break;
            case RESUME_MENU_DIALOG_ID:
                if (NetworkManager.instance().isDataUp()) {
                    TransferManager.instance().resumeResumableTransfers();
                } else {
                    UIUtils.showShortMessage(getActivity(), R.string.please_check_connection_status_before_resuming_download);
                }
                break;
            }
            setupAdapter();
        }
    }

    private void showContextMenu() {
        MenuItem clear = new MenuItem(CLEAR_MENU_DIALOG_ID, R.string.transfers_context_menu_clear_finished, R.drawable.contextmenu_icon_remove_transfer);
        MenuItem pause = new MenuItem(PAUSE_MENU_DIALOG_ID, R.string.transfers_context_menu_pause_stop_all_transfers, R.drawable.contextmenu_icon_pause_transfer);
        MenuItem resume = new MenuItem(RESUME_MENU_DIALOG_ID, R.string.transfers_context_resume_all_torrent_transfers, R.drawable.contextmenu_icon_play);
        
        List<MenuItem> dlgActions = new ArrayList<MenuItem>();
        
        final List<Transfer> transfers = TransferManager.instance().getTransfers();
        
        if (transfers != null && transfers.size() > 0) {
            if (someTransfersComplete(transfers)) {
                dlgActions.add(clear);
            }
            
            if (someTransfersActive(transfers)) {
                dlgActions.add(pause);
            }
            
            if (someTransfersInactive(transfers)) {
                dlgActions.add(resume);
            }
        }
        
        if (dlgActions.size() > 0) {
            MenuDialog dlg = MenuDialog.newInstance(TRANSFERS_DIALOG_ID, dlgActions);
            dlg.show(getFragmentManager());
        }
    }

    private boolean someTransfersInactive(List<Transfer> transfers) {
        for (Transfer t : transfers) {
            if (t instanceof BittorrentDownload) {
                BittorrentDownload bt = (BittorrentDownload) t;
                if (!bt.isDownloading() && !bt.isSeeding()) {
                    return true;
                }
            } else if (t instanceof HttpDownload) {
                HttpDownload ht = (HttpDownload) t;
                if (ht.isComplete() || !ht.isDownloading()) {
                    return true;
                }
            } else if (t instanceof YouTubeDownload) {
                YouTubeDownload yt = (YouTubeDownload) t;
                if (yt.isComplete() || !yt.isDownloading()) {
                    return true;
                }
                
            } else if (t instanceof SoundcloudDownload) {
                SoundcloudDownload sd = (SoundcloudDownload) t;
                if (sd.isComplete() || !sd.isDownloading()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean someTransfersComplete(List<Transfer> transfers) {
        for (Transfer t : transfers) {
            if (t.isComplete()) {
                return true; 
            }
        }
        return false;
    }
    
    private boolean someTransfersActive(List<Transfer> transfers) {
        for (Transfer t : transfers) {
            if (t instanceof BittorrentDownload) {
                BittorrentDownload bt = (BittorrentDownload) t;
                if (bt.isDownloading() || bt.isSeeding()) {
                    return true;
                }
            } else if (t instanceof HttpDownload) {
                HttpDownload ht = (HttpDownload) t;
                if (ht.isDownloading()) {
                    return true;
                }
            } else if (t instanceof YouTubeDownload) {
                YouTubeDownload yt = (YouTubeDownload) t;
                if (yt.isDownloading()) {
                    return true;
                }
            } else if (t instanceof SoundcloudDownload) {
                SoundcloudDownload sd = (SoundcloudDownload) t;
                if (sd.isDownloading()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void startTransferFromURL() {
        String text = addTransferUrlTextView.getText();
        if (!StringUtils.isNullOrEmpty(text) && (text.startsWith("magnet") || text.startsWith("http"))) {
            toggleAddTransferControls();
            if (text.startsWith("http") && text.contains("youtube") || text.contains("soundcloud")) {
                UIUtils.showLongMessage(getActivity(), R.string.cloud_downloads_coming);
                //TODO!
            } else if (text.startsWith("http")) { //magnets are automatically started if found on the clipboard by autoPasteMagnetOrURL
                TransferManager.instance().downloadTorrent(text.trim());
                UIUtils.showLongMessage(getActivity(), R.string.torrent_url_added);
            }
            addTransferUrlTextView.setText("");
        } else {
            UIUtils.showLongMessage(getActivity(), R.string.please_enter_valid_url);
        }
    }

    private void autoPasteMagnetOrURL() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData primaryClip = clipboard.getPrimaryClip();
        if (primaryClip != null) {
            Item itemAt = primaryClip.getItemAt(0);
            String text = (String) itemAt.getText();
            if (!StringUtils.isNullOrEmpty(text)) {
                if (text.startsWith("http")) {
                    addTransferUrlTextView.requestFocus();
                    addTransferUrlTextView.setText(text.trim());
                } else if (text.startsWith("magnet")) {
                    addTransferUrlTextView.setText(text.trim());
                    TransferManager.instance().downloadTorrent(text.trim());
                    toggleAddTransferControls();
                    UIUtils.showLongMessage(getActivity(), R.string.magnet_url_added);
                }
            }
        }
    }

    private void toggleAddTransferControls() {
        if (addTransferUrlTextView.getVisibility() == View.GONE) {
            addTransferUrlTextView.setVisibility(View.VISIBLE);
            autoPasteMagnetOrURL();
            showAddTransfersKeyboard();
        } else {
            addTransferUrlTextView.setVisibility(View.GONE);
            addTransferUrlTextView.setText("");
            hideAddTransfersKeyboard();
        }
    }

    private void showAddTransfersKeyboard() {
        if (addTransferUrlTextView.getText().startsWith("http")) {
            LOG.debug("addTransferUrlTextView is focusable? " + addTransferUrlTextView.isFocusable());
            LOG.debug("addTransferUrlTextView is focused? " + addTransferUrlTextView.isFocused());
            InputMethodManager imm = (InputMethodManager) addTransferUrlTextView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(addTransferUrlTextView, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    private void hideAddTransfersKeyboard() {
        InputMethodManager imm = (InputMethodManager) addTransferUrlTextView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(addTransferUrlTextView.getWindowToken(), 0);
    }

    private static final class TransferComparator implements Comparator<Transfer> {
        public int compare(Transfer lhs, Transfer rhs) {
            try {
                return -lhs.getDateCreated().compareTo(rhs.getDateCreated());
            } catch (Throwable e) {
                // ignore, not really super important
            }
            return 0;
        }
    }

    public static enum TransferStatus {
        ALL, DOWNLOADING, COMPLETED
    }

    private static final class ButtonAddTransferListener extends ClickAdapter<TransfersFragment> {

        public ButtonAddTransferListener(TransfersFragment f) {
            super(f);
        }

        @Override
        public void onClick(TransfersFragment f, View v) {
            f.toggleAddTransferControls();
        }
    }
    
    private static final class ButtonMenuListener extends ClickAdapter<TransfersFragment> {

        public ButtonMenuListener(TransfersFragment f) {
            super(f);
        }

        @Override
        public void onClick(TransfersFragment f, View v) {
            f.showContextMenu();
        }
    }
    
    private static final class AddTransferTextListener extends ClickAdapter<TransfersFragment> implements OnItemClickListener, OnActionListener {

        public AddTransferTextListener(TransfersFragment owner) {
            super(owner);
        }

        @Override
        public boolean onKey(TransfersFragment owner, View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                owner.startTransferFromURL();
                return true;
            }
            return false;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (Ref.alive(ownerRef)) {
                TransfersFragment owner = ownerRef.get();
                owner.startTransferFromURL();
            }
        }

        @Override
        public void onClear(View v) {
            if (Ref.alive(ownerRef)) {
                //TransfersFragment owner = ownerRef.get();
                //might clear.
                LOG.debug("onClear");
            }
        }

        @Override
        public void onTextChanged(View v, String str) {
        }
    }

    private static final class ButtonTabListener extends ClickAdapter<TransfersFragment> {

        private final TransferStatus status;

        public ButtonTabListener(TransfersFragment f, TransferStatus status) {
            super(f);
            this.status = status;
        }

        @Override
        public void onClick(TransfersFragment f, View v) {
            f.selectedStatus = status;
            f.onTime();
        }
    }
}