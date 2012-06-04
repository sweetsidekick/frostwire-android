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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.adapters.SearchResultListAdapter;
import com.frostwire.android.gui.search.LocalSearchEngine;
import com.frostwire.android.gui.transfers.DownloadTransfer;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractListFragment;
import com.frostwire.android.gui.views.Refreshable;
import com.frostwire.android.gui.views.SearchInputView;
import com.frostwire.android.gui.views.SearchInputView.OnSearchListener;
import com.google.ads.AdSize;
import com.google.ads.AdView;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SearchFragment extends AbstractListFragment implements Refreshable {

    @SuppressWarnings("unused")
    private static final String TAG = "FW.SearchFragment";

    private SearchInputView searchInput;

    private SearchResultListAdapter adapter;

    private int mediaTypeId;
    private ProgressDialog progressDlg;

    private AdView adView;

    public SearchFragment() {
        super(R.layout.fragment_search);
        mediaTypeId = ConfigurationManager.instance().getLastMediaTypeFilter();
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
    public void refresh() {
        if (adapter != null) {
            if (LocalSearchEngine.instance().getCurrentResultsCount() != adapter.getList().size()) {
                adapter.updateList(LocalSearchEngine.instance().pollCurrentResults());
                adapter.filter(mediaTypeId);
            }
        } else {
            setupAdapter();
        }

        if (adapter != null && adapter.getCount() > 0) {
            hideProgressDialog();

            adjustLoadingViewVisibility(getView());
        }
    }

    @Override
    protected void initComponents(final View view) {
        searchInput = findView(view, R.id.fragment_search_input);
        searchInput.setOnSearchListener(new OnSearchListener() {
            public void onSearch(View v, String query, int mediaTypeId) {
                SearchFragment.this.mediaTypeId = mediaTypeId;
                switchView(view, android.R.id.list);
                clearAdapter();
                showProgressDialog();
                LocalSearchEngine.instance().performSearch(query);
                setupAdapter();
            }

            public void onMediaTypeSelected(View v, int mediaTypeId) {
                SearchFragment.this.mediaTypeId = mediaTypeId;
                if (adapter != null) {
                    adapter.filter(mediaTypeId);
                }
            }

            public void onClear(View v) {
                switchView(view, R.id.fragment_search_promos);
                LocalSearchEngine.instance().cancelSearch();
                clearAdapter();
            }
        });

        LinearLayout llayout = findView(view, R.id.fragment_search_adview_layout);
        adView = new AdView(this.getActivity(), AdSize.SMART_BANNER, Constants.ADMOB_PUBLISHER_ID);
        adView.setVisibility(View.GONE);
        llayout.addView(adView, 0);
        
        adjustLoadingViewVisibility(view);

        if (LocalSearchEngine.instance().getCurrentResultsCount() > 0) {
            setupAdapter();
            switchView(view, android.R.id.list);
        } else {
            switchView(view, R.id.fragment_search_promos);
        }
    }

    @Override
    protected void dismissDialogs() {
        super.dismissDialogs();

        searchInput.hideQuickAction();

        if (adapter != null) {
            adapter.dismissDialogs();
        }
    }

    private void setupAdapter() {
        if (LocalSearchEngine.instance().getCurrentResultsCount() > 0) {
            adapter = new SearchResultListAdapter(getActivity(), LocalSearchEngine.instance().pollCurrentResults()) {
                @Override
                protected void onTransferStarted(DownloadTransfer transfer) {
                    LocalSearchEngine.instance().cancelSearch();
                }
            };
            adapter.filter(mediaTypeId);

            if (adapter.getCount() > 0) {
                hideProgressDialog();
            }

            setListAdapter(adapter);
        }
    }

    private void clearAdapter() {
        setListAdapter(null);
        if (adapter != null) {
            adapter.clear();
            adapter = null;
        }
        adView.setVisibility(View.GONE);
        adjustLoadingViewVisibility(getView());
    }

    private void showProgressDialog() {
        hideProgressDialog();

        progressDlg = new ProgressDialog(getActivity());
        progressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDlg.setMessage(getString(R.string.searching_indeterminate));
        progressDlg.setCancelable(false);

        progressDlg.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalSearchEngine.instance().cancelSearch();
                hideProgressDialog();
            }
        });

        trackDialog(progressDlg).show();
    }

    private void hideProgressDialog() {
        if (progressDlg != null) {
            try {
                progressDlg.dismiss();
                progressDlg = null;
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    private void switchView(View v, int id) {
        FrameLayout frameLayout = findView(v, R.id.fragment_search_framelayout);

        int childCount = frameLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = frameLayout.getChildAt(i);
            childAt.setVisibility((childAt.getId() == id) ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void adjustLoadingViewVisibility(View v) {
        int visibility = View.GONE;

        if (adapter != null && LocalSearchEngine.instance().getDownloadTasksCount() > 0) {
            visibility = View.VISIBLE;
        }

        findView(v, R.id.fragment_search_loading_view).setVisibility(visibility);
    }
}