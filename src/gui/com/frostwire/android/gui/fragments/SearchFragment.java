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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.PromotionsHandler;
import com.frostwire.android.gui.PromotionsHandler.Slide;
import com.frostwire.android.gui.adapters.SearchResultListAdapter2;
import com.frostwire.android.gui.search.LocalSearchEngine;
import com.frostwire.android.gui.transfers.DownloadTransfer;
import com.frostwire.android.gui.transfers.ExistingDownload;
import com.frostwire.android.gui.transfers.InvalidTransfer;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractListFragment;
import com.frostwire.android.gui.views.NewTransferDialog;
import com.frostwire.android.gui.views.NewTransferDialog.OnYesNoListener;
import com.frostwire.android.gui.views.PromotionsView;
import com.frostwire.android.gui.views.PromotionsView.OnPromotionClickListener;
import com.frostwire.android.gui.views.Refreshable;
import com.frostwire.android.gui.views.SearchInputView;
import com.frostwire.android.gui.views.SearchInputView.OnSearchListener;
import com.frostwire.search.FileSearchResult;
import com.frostwire.search.SearchResult;
import com.google.ads.AdSize;
import com.google.ads.AdView;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SearchFragment extends AbstractListFragment implements Refreshable, MainFragment {

    private static final String TAG = "FW.SearchFragment";

    private SearchInputView searchInput;

    private SearchResultListAdapter2 adapter;

    private int mediaTypeId;
    private ProgressDialog progressDlg;
    private int progress;

    private AdView adView;

    private TextView header;
    private PromotionsView promotions;

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
        }

        adjustDeepSearchProgress(getView());
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
        header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.search);

        return header;
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
                updateHint(mediaTypeId);
            }

            public void onMediaTypeSelected(View v, int mediaTypeId) {
                SearchFragment.this.mediaTypeId = mediaTypeId;
                updateHint(mediaTypeId);

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

        adjustDeepSearchProgress(view);

        if (LocalSearchEngine.instance().getCurrentResultsCount() > 0) {
            setupAdapter();
            switchView(view, android.R.id.list);
        } else {
            switchView(view, R.id.fragment_search_promos);
        }

        promotions = findView(view, R.id.fragment_search_promos);
        promotions.setOnPromotionClickListener(new OnPromotionClickListener() {
            @Override
            public void onPromotionClick(PromotionsView v, Slide slide) {
                if (slide != null) {
                    startPromotionDownload(slide);
                }
            }
        });

        updateHint(mediaTypeId);
    }

    private void setupAdapter() {
        if (LocalSearchEngine.instance().getCurrentResultsCount() > 0) {
            adapter = new SearchResultListAdapter2(getActivity(), LocalSearchEngine.instance().pollCurrentResults()) {
                @Override
                protected void onStartTransfer(SearchResult sr) {
                    startTransfer(sr, getString(R.string.download_added_to_queue));
                }
            };
            adapter.filter(mediaTypeId);

            if (adapter.getCount() > 0) {
                hideProgressDialog();
            }

            setListAdapter(adapter);
        }
    }

    private void startTransfer(final SearchResult sr, final String toastMessage) {
        OnYesNoListener listener = new OnYesNoListener() {
            public void onYes(NewTransferDialog dialog) {
                startDownload(sr, toastMessage);
            }

            public void onNo(NewTransferDialog dialog) {
            }
        };

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG)) {
            if (sr instanceof FileSearchResult) {
                NewTransferDialog dlg = new NewTransferDialog();
                dlg.setSearchResult((FileSearchResult) sr);
                dlg.setListener(listener);
                dlg.show(getFragmentManager());
            }
        } else {
            listener.onYes(null);
        }
    }

    private void startDownload(final SearchResult sr, final String toastMessage) {
        AsyncTask<Void, Void, DownloadTransfer> task = new AsyncTask<Void, Void, DownloadTransfer>() {

            @Override
            protected DownloadTransfer doInBackground(Void... params) {
                DownloadTransfer transfer = null;
                try {
                    transfer = TransferManager.instance().download(sr);
                } catch (Throwable e) {
                    Log.e(TAG, "Error adding new download from result: " + sr, e);
                }

                return transfer;
            }

            @Override
            protected void onPostExecute(DownloadTransfer transfer) {
                if (!(transfer instanceof InvalidTransfer)) {
                    UIUtils.showShortMessage(getActivity(), toastMessage);
                } else {
                    if (transfer instanceof ExistingDownload) {
                        //nothing happens here, the user should just see the transfer
                        //manager and we avoid adding the same transfer twice.
                    } else {
                        UIUtils.showShortMessage(getActivity(), ((InvalidTransfer) transfer).getReasonResId());
                    }
                }
            }

        };

        UIUtils.showTransfersOnDownloadStart(getActivity());
        task.execute();
    }

    private void startPromotionDownload(Slide slide) {
        SearchResult sr = new PromotionsHandler().buildSearchResult(slide);
        if (sr == null) {

            //check if there is a URL available to open a web browser.
            if (slide.url != null) {
                Intent i = new Intent("android.intent.action.VIEW", Uri.parse(slide.url));
                getActivity().startActivity(i);
            }

            return;
        }
        startTransfer(sr, getString(R.string.downloading_promotion, sr.getDisplayName()));
    }

    private void clearAdapter() {
        setListAdapter(null);
        if (adapter != null) {
            adapter.clear();
            adapter = null;
        }
        adView.setVisibility(View.GONE);
        adjustDeepSearchProgress(getView());
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

    private void updateHint(int mediaTypeId) {
        String searchBoxHint = getActivity().getString(R.string.search_label) + " ";
        searchBoxHint += UIUtils.getMediaTypeString(getActivity().getResources(), mediaTypeId);
        searchInput.updateHint(searchBoxHint);
    }

    private void adjustDeepSearchProgress(View v) {
        int visibility;

        if (adapter != null && false) {//LocalSearchEngine.instance().getDownloadTasksCount() > 0) {
            progress = (progress + 20) % 100;
            if (progress == 0) {
                progress = 10;
            }
            visibility = View.VISIBLE;
        } else {
            progress = 0;
            visibility = View.GONE;
        }

        if (v != null) {
            ProgressBar progressBar = findView(v, R.id.fragment_search_deepsearch_progress);
            progressBar.setProgress(progress);
            progressBar.setVisibility(visibility);
        }
    }
}