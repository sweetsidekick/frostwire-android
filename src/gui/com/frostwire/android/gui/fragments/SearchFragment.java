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

package com.frostwire.android.gui.fragments;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.LocalSearchEngine;
import com.frostwire.android.gui.adapters.SearchResultListAdapter;
import com.frostwire.android.gui.adapters.SearchResultListAdapter.FilteredSearchResults;
import com.frostwire.android.gui.transfers.DownloadTransfer;
import com.frostwire.android.gui.transfers.ExistingDownload;
import com.frostwire.android.gui.transfers.HttpSlideSearchResult;
import com.frostwire.android.gui.transfers.InvalidTransfer;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractListFragment;
import com.frostwire.android.gui.views.NewTransferDialog;
import com.frostwire.android.gui.views.NewTransferDialog.OnYesNoListener;
import com.frostwire.android.gui.views.PromotionsView;
import com.frostwire.android.gui.views.PromotionsView.OnPromotionClickListener;
import com.frostwire.android.gui.views.SearchInputView;
import com.frostwire.android.gui.views.SearchInputView.OnSearchListener;
import com.frostwire.android.gui.views.SearchProgressView;
import com.frostwire.frostclick.Slide;
import com.frostwire.frostclick.SlideList;
import com.frostwire.frostclick.TorrentPromotionSearchResult;
import com.frostwire.logging.Logger;
import com.frostwire.search.FileSearchResult;
import com.frostwire.search.HttpSearchResult;
import com.frostwire.search.SearchManagerListener;
import com.frostwire.search.SearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.search.torrent.TorrentCrawledSearchResult;
import com.frostwire.search.torrent.TorrentSearchResult;
import com.frostwire.util.HttpClient;
import com.frostwire.util.HttpClientFactory;
import com.frostwire.util.JsonUtils;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class SearchFragment extends AbstractListFragment implements MainFragment {

    private static final Logger LOG = Logger.getLogger(SearchFragment.class);

    private SearchResultListAdapter adapter;
    private List<Slide> slides;

    private SearchInputView searchInput;
    private ProgressBar deepSearchProgress;
    private PromotionsView promotions;
    private SearchProgressView searchProgress;

    private final FileTypeCounter fileTypeCounter;

    public SearchFragment() {
        super(R.layout.fragment_search);
        fileTypeCounter = new FileTypeCounter();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupAdapter();

        if (slides != null) {
            promotions.setSlides(slides);
        } else {
            new LoadSlidesTask(this).execute();
        }

        setRetainInstance(true);
    }

    @Override
    public View getHeader(Activity activity) {

        LayoutInflater inflater = LayoutInflater.from(activity);
        TextView header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.search);

        return header;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter != null && (adapter.getCount() > 0 || adapter.getTotalCount() > 0)) {
            refreshFileTypeCounters(true);
        }
    }

    @Override
    protected void initComponents(final View view) {

        searchInput = findView(view, R.id.fragment_search_input);
        searchInput.setOnSearchListener(new OnSearchListener() {
            public void onSearch(View v, String query, int mediaTypeId) {
                performSearch(query, mediaTypeId);
            }

            public void onMediaTypeSelected(View v, int mediaTypeId) {
                adapter.setFileType(mediaTypeId);
                showSearchView(view);
            }

            public void onClear(View v) {
                cancelSearch(view);
            }
        });

        deepSearchProgress = findView(view, R.id.fragment_search_deepsearch_progress);
        deepSearchProgress.setVisibility(View.GONE);

        promotions = findView(view, R.id.fragment_search_promos);
        promotions.setOnPromotionClickListener(new OnPromotionClickListener() {
            @Override
            public void onPromotionClick(PromotionsView v, Slide slide) {
                startPromotionDownload(slide);
            }
        });

        searchProgress = findView(view, R.id.fragment_search_search_progress);
        searchProgress.setCancelOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LocalSearchEngine.instance().isSearchFinished()) {
                    performSearch(searchInput.getText(), adapter.getFileType()); // retry
                } else {
                    cancelSearch(view);
                }
            }
        });

        showSearchView(view);
    }

    private void setupAdapter() {
        if (adapter == null) {
            adapter = new SearchResultListAdapter(getActivity()) {
                @Override
                protected void searchResultClicked(SearchResult sr) {
                    startTransfer(sr, getString(R.string.download_added_to_queue));
                }
            };
            setListAdapter(adapter);

            LocalSearchEngine.instance().registerListener(new SearchManagerListener() {
                @Override
                public void onResults(SearchPerformer performer, final List<? extends SearchResult> results) {
                    @SuppressWarnings("unchecked")
                    FilteredSearchResults fsr = adapter.filter((List<SearchResult>) results);
                    final List<SearchResult> filteredList = fsr.filtered;

                    fileTypeCounter.add(fsr);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.addResults(results, filteredList);
                            showSearchView(getView());
                            refreshFileTypeCounters(true);
                        }
                    });
                }

                @Override
                public void onFinished(long token) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            searchProgress.setProgressEnabled(false);
                            deepSearchProgress.setVisibility(View.GONE);
                        }
                    });
                }
            });
        }
    }

    private void refreshFileTypeCounters(boolean fileTypeCountersVisible) {
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_APPLICATIONS, fileTypeCounter.fsr.numApplications);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_AUDIO, fileTypeCounter.fsr.numAudio);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_DOCUMENTS, fileTypeCounter.fsr.numDocuments);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_PICTURES, fileTypeCounter.fsr.numPictures);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_TORRENTS, fileTypeCounter.fsr.numTorrents);
        searchInput.updateFileTypeCounter(Constants.FILE_TYPE_VIDEOS, fileTypeCounter.fsr.numVideo);

        searchInput.setFileTypeCountersVisible(fileTypeCountersVisible);
    }

    private void performSearch(String query, int mediaTypeId) {
        adapter.clear();
        adapter.setFileType(mediaTypeId);
        fileTypeCounter.clear();
        refreshFileTypeCounters(false);
        LocalSearchEngine.instance().performSearch(query);
        searchProgress.setProgressEnabled(true);
        showSearchView(getView());
        UXStats.instance().log(UXAction.SEARCH_STARTED_ENTER_KEY);

    }

    private void cancelSearch(View view) {
        adapter.clear();
        fileTypeCounter.clear();
        refreshFileTypeCounters(false);
        LocalSearchEngine.instance().cancelSearch();
        searchProgress.setProgressEnabled(false);
        showSearchView(getView());
    }

    private void showSearchView(View view) {
        if (LocalSearchEngine.instance().isSearchStopped()) {
            switchView(view, R.id.fragment_search_promos);
            deepSearchProgress.setVisibility(View.GONE);
        } else {
            if (adapter != null && adapter.getCount() > 0) {
                switchView(view, android.R.id.list);
                deepSearchProgress.setVisibility(LocalSearchEngine.instance().isSearchFinished() ? View.GONE : View.VISIBLE);
            } else {
                switchView(view, R.id.fragment_search_search_progress);
                deepSearchProgress.setVisibility(View.GONE);
            }
        }
        searchProgress.setProgressEnabled(!LocalSearchEngine.instance().isSearchFinished());
    }

    private void switchView(View v, int id) {
        if (v != null) {
            FrameLayout frameLayout = findView(v, R.id.fragment_search_framelayout);

            int childCount = frameLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = frameLayout.getChildAt(i);
                childAt.setVisibility((childAt.getId() == id) ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    private void startTransfer(final SearchResult sr, final String toastMessage) {
        OnYesNoListener listener = new OnYesNoListener() {
            public void onYes(NewTransferDialog dialog) {
                startDownload(sr, toastMessage);
                uxLogAction(sr);
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
        AsyncTask<Void, Void, DownloadTransfer> task = new StartDownloadAsyncTask(getActivity(), sr, toastMessage);
        UIUtils.showTransfersOnDownloadStart(getActivity());
        task.execute();
    }
    
    private void startPromotionDownload(Slide slide) {
        SearchResult sr = null;

        switch (slide.method) {
        case Slide.DOWNLOAD_METHOD_TORRENT:
            sr = new TorrentPromotionSearchResult(slide);
            break;
        case Slide.DOWNLOAD_METHOD_HTTP:
            sr = new HttpSlideSearchResult(slide);
            break;
        default:
            sr = null;
            break;
        }
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

    private void uxLogAction(SearchResult sr) {
        UXStats.instance().log(UXAction.SEARCH_RESULT_CLICKED);

        if (sr instanceof HttpSearchResult) {
            UXStats.instance().log(UXAction.DOWNLOAD_CLOUD_FILE);
        } else if (sr instanceof TorrentSearchResult) {
            if (sr instanceof TorrentCrawledSearchResult) {
                UXStats.instance().log(UXAction.DOWNLOAD_PARTIAL_TORRENT_FILE);
            } else {
                UXStats.instance().log(UXAction.DOWNLOAD_FULL_TORRENT_FILE);
            }
        }
    }
    
    private static class StartDownloadAsyncTask extends AsyncTask<Void, Void, DownloadTransfer> {
        private final WeakReference<Activity> activity;
        private final SearchResult sr;
        private final String toastMessage;
        
        public StartDownloadAsyncTask(final Activity activity, final SearchResult sr, final String toastMessage) {
            this.activity = new WeakReference<Activity>(activity);
            this.sr = sr;
            this.toastMessage = toastMessage;
        }
        
        @Override
        protected DownloadTransfer doInBackground(Void... params) {
            DownloadTransfer transfer = null;
            try {
                transfer = TransferManager.instance().download(sr);
            } catch (Throwable e) {
                LOG.warn("Error adding new download from result: " + sr, e);
            }

            return transfer;
        }

        @Override
        protected void onPostExecute(DownloadTransfer transfer) {
            if (transfer != null) {
                if (!(transfer instanceof InvalidTransfer)) {
                    UIUtils.showShortMessage(activity.get(), toastMessage);
                } else {
                    if (transfer instanceof ExistingDownload) {
                        //nothing happens here, the user should just see the transfer
                        //manager and we avoid adding the same transfer twice.
                    } else {
                        UIUtils.showShortMessage(activity.get(), ((InvalidTransfer) transfer).getReasonResId());
                    }
                }
            }
        }

    };

    private static class LoadSlidesTask extends AsyncTask<Void, Void, List<Slide>> {

        private final WeakReference<SearchFragment> fragment;

        public LoadSlidesTask(SearchFragment fragment) {
            this.fragment = new WeakReference<SearchFragment>(fragment);
        }

        @Override
        protected List<Slide> doInBackground(Void... params) {
            try {
                HttpClient http = HttpClientFactory.newInstance();
                String url = String.format("%s?from=android&fw=%s&sdk=%s", Constants.SERVER_PROMOTIONS_URL, Constants.FROSTWIRE_VERSION_STRING, Build.VERSION.SDK_INT);
                String json = http.get(url);
                SlideList slides = JsonUtils.toObject(json, SlideList.class);
                return slides.slides;
            } catch (Throwable e) {
                LOG.error("Error loading slides from url", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Slide> result) {
            SearchFragment f = fragment.get();
            if (f != null) {
                f.slides = result;
                f.promotions.setSlides(result);
            }
        }
    }

    private static final class FileTypeCounter {

        private final FilteredSearchResults fsr = new FilteredSearchResults();

        public void add(FilteredSearchResults fsr) {
            this.fsr.numAudio += fsr.numAudio;
            this.fsr.numApplications += fsr.numApplications;
            this.fsr.numDocuments += fsr.numDocuments;
            this.fsr.numPictures += fsr.numPictures;
            this.fsr.numTorrents += fsr.numTorrents;
            this.fsr.numVideo += fsr.numVideo;
        }

        public void clear() {
            this.fsr.numAudio = 0;
            this.fsr.numApplications = 0;
            this.fsr.numDocuments = 0;
            this.fsr.numPictures = 0;
            this.fsr.numTorrents = 0;
            this.fsr.numVideo = 0;
        }
    }
}