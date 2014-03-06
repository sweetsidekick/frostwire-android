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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.gui.adapters.TransferListAdapter;
import com.frostwire.android.gui.transfers.Transfer;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractExpandableListFragment;
import com.frostwire.android.gui.views.Refreshable;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class TransfersFragment2 extends AbstractExpandableListFragment implements Refreshable, MainFragment {

    private static final String SELECTED_STATUS_STATE_KEY = "selected_status";

    private final Comparator<Transfer> transferComparator;

    private Button buttonSelectAll;
    private Button buttonSelectDownloading;
    private Button buttonSelectCompleted;
    private TextView textDownloads;
    private TextView textUploads;

    private TransferListAdapter adapter;

    private TextView header;

    private TransferStatus selectedStatus;

    public TransfersFragment2() {
        super(R.layout.fragment_transfers2);

        this.transferComparator = new TransferComparator();

        selectedStatus = TransferStatus.ALL;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof AbstractActivity) {
            ((AbstractActivity) getActivity()).addRefreshable(this);
        }

        UIUtils.initSupportFrostWire(getActivity(), R.id.activity_mediaplayer_donations_view_placeholder);

        if (savedInstanceState != null) {
            selectedStatus = TransferStatus.valueOf(savedInstanceState.getString(SELECTED_STATUS_STATE_KEY, TransferStatus.ALL.name()));
        }
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
    public void refresh() {
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
        header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.transfers);

        return header;
    }

    @Override
    protected void initComponents(View v) {
        buttonSelectAll = findView(v, R.id.fragment_transfers_button_select_all);
        buttonSelectAll.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectedStatus = TransferStatus.ALL;
                refresh();
            }
        });
        buttonSelectDownloading = findView(v, R.id.fragment_transfers_button_select_downloading);
        buttonSelectDownloading.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectedStatus = TransferStatus.DOWNLOADING;
                refresh();
            }
        });
        buttonSelectCompleted = findView(v, R.id.fragment_transfers_button_select_completed);
        buttonSelectCompleted.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectedStatus = TransferStatus.COMPLETED;
                refresh();
            }
        });

        textDownloads = findView(v, R.id.fragment_transfers_text_downloads);
        textUploads = findView(v, R.id.fragment_transfers_text_uploads);
    }

    private void setupAdapter() {
        List<Transfer> transfers = filter(TransferManager.instance().getTransfers(), selectedStatus);
        Collections.sort(transfers, transferComparator);
        adapter = new TransferListAdapter(TransfersFragment2.this.getActivity(), transfers);
        setListAdapter(adapter);
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

    private List<Transfer> filter(List<Transfer> transfers, TransferStatus status) {
        switch (status) {
        case DOWNLOADING:
            return Collections.emptyList();// transfers;
        case COMPLETED:
            return transfers;
        default:
            return transfers;
        }
    }

    private static enum TransferStatus {
        ALL, DOWNLOADING, COMPLETED
    }
}
