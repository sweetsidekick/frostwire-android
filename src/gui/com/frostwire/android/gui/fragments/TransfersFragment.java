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

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class TransfersFragment extends AbstractExpandableListFragment implements Refreshable, MainFragment {

    @SuppressWarnings("unused")
    private static final String TAG = "FW.TransfersFragment";

    private final Comparator<Transfer> transferComparator;

    private Button buttonPauseAll;
    private Button buttonClearComplete;
    private TextView textDownloads;
    private TextView textUploads;

    private TransferListAdapter adapter;

    private TextView header;

    public TransfersFragment() {
        super(R.layout.fragment_transfers);

        this.transferComparator = new TransferComparator();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        if (getActivity() instanceof AbstractActivity) {
            ((AbstractActivity) getActivity()).addRefreshable(this);
        }
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
            List<Transfer> transfers = TransferManager.instance().getTransfers();
            Collections.sort(transfers, transferComparator);
            adapter.updateList(transfers);
        } else {
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
        buttonPauseAll = findView(v, R.id.fragment_transfers_button_pauseall);
        buttonPauseAll.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                UIUtils.showYesNoDialog(getActivity(), R.string.stop_all_transfers, R.string.are_you_sure, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TransferManager.instance().stopHttpTransfers();
                        TransferManager.instance().pauseTorrents();
                    }
                });
            }
        });
        buttonClearComplete = findView(v, R.id.fragment_transfers_button_clearcomplete);
        buttonClearComplete.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                UIUtils.showYesNoDialog(getActivity(), R.string.clear_complete_transfers, R.string.are_you_sure, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TransferManager.instance().clearComplete();
                    }
                });
            }
        });

        textDownloads = findView(v, R.id.fragment_transfers_text_downloads);
        textUploads = findView(v, R.id.fragment_transfers_text_uploads);
    }

    private void setupAdapter() {
        List<Transfer> transfers = TransferManager.instance().getTransfers();
        Collections.sort(transfers, transferComparator);
        adapter = new TransferListAdapter(this.getActivity(), transfers);
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
}
