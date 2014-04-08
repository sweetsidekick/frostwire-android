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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.adapters.TransferListAdapter;
import com.frostwire.android.gui.dialogs.MenuDialog;
import com.frostwire.android.gui.dialogs.MenuDialog.MenuItem;
import com.frostwire.android.gui.transfers.Transfer;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractDialog.OnDialogClickListener;
import com.frostwire.android.gui.views.AbstractFragment;
import com.frostwire.android.gui.views.TimerObserver;
import com.frostwire.android.gui.views.TimerService;
import com.frostwire.android.gui.views.TimerSubscription;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class TransfersFragment extends AbstractFragment implements TimerObserver, MainFragment, OnDialogClickListener {

    private static final String SELECTED_STATUS_STATE_KEY = "selected_status";

    private final Comparator<Transfer> transferComparator;

    private Button buttonSelectAll;
    private Button buttonSelectDownloading;
    private Button buttonSelectCompleted;
    private ExpandableListView list;
    private TextView textDownloads;
    private TextView textUploads;

    private TransferListAdapter adapter;

    private TransferStatus selectedStatus;
    
    private TimerSubscription subscription;

    public TransfersFragment() {
        super(R.layout.fragment_transfers);

        this.transferComparator = new TransferComparator();

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
    public void onResume() {
        super.onResume();
        if (hasTransfersDownloading()) {
            buttonSelectDownloading.performClick();
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
        buttonMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showContextMenu();
            }
        });

        return header;
    }

    @Override
    protected void initComponents(View v) {
        buttonSelectAll = findView(v, R.id.fragment_transfers_button_select_all);
        buttonSelectAll.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectedStatus = TransferStatus.ALL;
                onTime();
            }
        });
        buttonSelectDownloading = findView(v, R.id.fragment_transfers_button_select_downloading);
        buttonSelectDownloading.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectedStatus = TransferStatus.DOWNLOADING;
                onTime();
            }
        });
        buttonSelectCompleted = findView(v, R.id.fragment_transfers_button_select_completed);
        buttonSelectCompleted.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectedStatus = TransferStatus.COMPLETED;
                onTime();
            }
        });
        
        list = findView(v, R.id.fragment_transfers_list);

        textDownloads = findView(v, R.id.fragment_transfers_text_downloads);
        textUploads = findView(v, R.id.fragment_transfers_text_uploads);
    }
    
    private boolean hasTransfersDownloading() {
        List<Transfer> transfers = filter(TransferManager.instance().getTransfers(), TransferStatus.DOWNLOADING);
        return transfers != null && !transfers.isEmpty();
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
    private static final int STOP_MENU_DIALOG_ID = 1;
    
    @Override
    public void onDialogClick(String tag, int which) {
        if (tag.equals(TRANSFERS_DIALOG_ID)) {
            switch (which) {
            case CLEAR_MENU_DIALOG_ID:
                TransferManager.instance().clearComplete();
                break;
            case STOP_MENU_DIALOG_ID:
                TransferManager.instance().stopHttpTransfers();
                TransferManager.instance().pauseTorrents();
                break;
            }
        }
    }

    private void showContextMenu() {

        MenuItem clear = new MenuItem(CLEAR_MENU_DIALOG_ID, R.string.transfers_context_menu_clear_finished, R.drawable.contextmenu_icon_remove_transfer);
        MenuItem stop = new MenuItem(STOP_MENU_DIALOG_ID, R.string.transfers_context_menu_stop_delete_data, R.drawable.contextmenu_icon_stop_transfer);

        MenuDialog dlg = MenuDialog.newInstance(TRANSFERS_DIALOG_ID, Arrays.asList(clear, stop));
        dlg.show(getFragmentManager());
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

    private static enum TransferStatus {
        ALL, DOWNLOADING, COMPLETED
    }
}
