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

package com.frostwire.android.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.Html;
import android.util.Log;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.CoreRuntimeException;
import com.frostwire.android.core.SearchEngine;
import com.frostwire.android.core.providers.UniversalStore.Torrents;
import com.frostwire.android.core.providers.UniversalStore.Torrents.TorrentFilesColumns;
import com.frostwire.android.util.Normalizer;
import com.frostwire.android.util.StringUtils;
import com.frostwire.android.util.concurrent.ExecutorsHelper;
import com.frostwire.search.SearchManager;
import com.frostwire.search.SearchManagerImpl;
import com.frostwire.search.SearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.search.SearchResultListener;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public final class LocalSearchEngine {

    private static final String TAG = "FW.LocalSearchEngine";

    private static final int MAX_TORRENT_DOWNLOADS = 2; // we are in a very constrained environment
    private static final ExecutorService downloads_torrents_executor; // enqueue the downloads tasks here

    private final Application context;

    // search constants
    private final int COUNT_DOWNLOAD_FOR_TORRENT_DEEP_SCAN;
    private final int COUNT_ROUNDS_FOR_TORRENT_DEEP_SCAN;
    private final int INTERVAL_MS_FOR_TORRENT_DEEP_SCAN;
    private final int MIN_SEEDS_FOR_TORRENT_DEEP_SCAN;
    private final int MAX_TORRENT_FILES_TO_INDEX;
    private final int FULLTEXT_SEARCH_RESULTS_LIMIT;

    private SearchResultListener listener;

    private final Object lockObj = new Object();

    private final SearchManager manager;
    private long currentSearchToken;

    static {
        downloads_torrents_executor = ExecutorsHelper.newFixedSizeThreadPool(MAX_TORRENT_DOWNLOADS, "DownloadTorrentsExecutor");
    }

    private static LocalSearchEngine instance;

    public synchronized static void create(Application context) {
        if (instance != null) {
            return;
        }
        instance = new LocalSearchEngine(context);
    }

    public static LocalSearchEngine instance() {
        if (instance == null) {
            throw new CoreRuntimeException("LocalSearchEngine not created");
        }
        return instance;
    }

    public LocalSearchEngine(Application context) {
        this.context = context;

        COUNT_DOWNLOAD_FOR_TORRENT_DEEP_SCAN = ConfigurationManager.instance().getInt(Constants.PREF_KEY_SEARCH_COUNT_DOWNLOAD_FOR_TORRENT_DEEP_SCAN);
        COUNT_ROUNDS_FOR_TORRENT_DEEP_SCAN = ConfigurationManager.instance().getInt(Constants.PREF_KEY_SEARCH_COUNT_ROUNDS_FOR_TORRENT_DEEP_SCAN);
        INTERVAL_MS_FOR_TORRENT_DEEP_SCAN = ConfigurationManager.instance().getInt(Constants.PREF_KEY_SEARCH_INTERVAL_MS_FOR_TORRENT_DEEP_SCAN);
        MIN_SEEDS_FOR_TORRENT_DEEP_SCAN = ConfigurationManager.instance().getInt(Constants.PREF_KEY_SEARCH_MIN_SEEDS_FOR_TORRENT_DEEP_SCAN);
        MAX_TORRENT_FILES_TO_INDEX = ConfigurationManager.instance().getInt(Constants.PREF_KEY_SEARCH_MAX_TORRENT_FILES_TO_INDEX);
        FULLTEXT_SEARCH_RESULTS_LIMIT = ConfigurationManager.instance().getInt(Constants.PREF_KEY_SEARCH_FULLTEXT_SEARCH_RESULTS_LIMIT);

        this.manager = new SearchManagerImpl();
        this.manager.registerListener(new SearchResultListener() {
            @Override
            public void onResults(SearchPerformer performer, List<? extends SearchResult> results) {
                if (listener != null && !performer.isStopped()) {
                    if (performer.getToken() == currentSearchToken) { // one more additional protection
                        listener.onResults(performer, results);
                    } else {
                        performer.stop(); // why? just in case there is an inner error in an alternative search manager
                    }
                }
            }

            @Override
            public void onFinished(SearchPerformer performer) {
                if (listener != null) {
                    listener.onFinished(performer);
                }
            }
        });
    }

    public void registerListener(SearchResultListener listener) {
        this.listener = listener;
    }

    public void performSearch(String query) {
        manager.stop(currentSearchToken);

        currentSearchToken = System.nanoTime();
        for (SearchEngine se : SearchEngine.getEngines()) {
            if (se.isEnabled()) {
                SearchPerformer p = se.getPerformer(currentSearchToken, query);
                manager.perform(p);
            }
        }
    }

    public void cancelSearch() {
        manager.stop(currentSearchToken);
        currentSearchToken = 0;
    }

    public boolean isSearchStopped() {
        return currentSearchToken == 0;
    }

    private void deepSearch(String query) {
        /*
        query = sanitize(query);

        int downloaded = 0;
        SystemClock.sleep(INTERVAL_MS_FOR_TORRENT_DEEP_SCAN);

        for (int i = 0; i < COUNT_ROUNDS_FOR_TORRENT_DEEP_SCAN && !task.isCancelled(); i++) {

            // scan results for actual torrents

            List<com.frostwire.android.gui.search.SearchResult> results = new ArrayList<com.frostwire.android.gui.search.SearchResult>(currentResults.size());
            synchronized (currentResults) {
                Iterator<com.frostwire.android.gui.search.SearchResult> it = currentResults.iterator();
                while (it.hasNext()) {
                    results.add(it.next());
                }
            }

            for (int j = 0; j < results.size() && downloaded < COUNT_DOWNLOAD_FOR_TORRENT_DEEP_SCAN && !task.isCancelled(); j++) {
                com.frostwire.android.gui.search.SearchResult sr = results.get(j);
                if (sr instanceof BittorrentWebSearchResult) {
                    BittorrentWebSearchResult bsr = (BittorrentWebSearchResult) sr;

                    if (bsr.getHash() != null && (bsr.getRank() > MIN_SEEDS_FOR_TORRENT_DEEP_SCAN) && !torrentIndexed(bsr)) {
                        if (!knownInfoHashes.contains(bsr.getHash())) {
                            knownInfoHashes.add(bsr.getHash());
                            downloaded++;

                            DownloadTorrentTask downloadTask = new DownloadTorrentTask(query, bsr, task);
                            downloadTask.setListener(new SearchTaskListener() {
                                @Override
                                public void onFinish(SearchTask task) {
                                    downloadTasks.remove(task);
                                }
                            });
                            downloadTasks.add(downloadTask);
                            downloads_torrents_executor.execute(downloadTask);
                        }
                    }
                }
            }

            SystemClock.sleep(INTERVAL_MS_FOR_TORRENT_DEEP_SCAN);
        }
        */
    }

    public int getIndexCount() {
        Cursor c = null;
        try {
            ContentResolver cr = context.getContentResolver();
            c = cr.query(Torrents.Media.CONTENT_URI, new String[] { TorrentFilesColumns._ID }, null, null, null);
            return c.getCount();
        } catch (Throwable e) {
            Log.e(TAG, "Error in torrents content provider", e);
            return 0;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public int clearIndex() {
        ContentResolver cr = context.getContentResolver();
        cr.delete(Torrents.Media.CONTENT_URI_SEARCH, null, null);
        return cr.delete(Torrents.Media.CONTENT_URI, null, null);
    }

    private void search(String query) {
        //Log.d(TAG, "Local search query: " + query);
        List<Integer> ids = new ArrayList<Integer>();

        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        try {
            String fts = buildFtsQuery(query);
            c = cr.query(Torrents.Media.CONTENT_URI_SEARCH, new String[] { "rowid" }, null, new String[] { fts }, " torrent_seeds DESC LIMIT " + FULLTEXT_SEARCH_RESULTS_LIMIT);
            while (c.moveToNext()) {
                ids.add(c.getInt(0));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        try {
            long start = System.currentTimeMillis();
            c = cr.query(Torrents.Media.CONTENT_URI, new String[] { TorrentFilesColumns.JSON }, "_id IN " + StringUtils.buildSet(ids), null, "torrent_seeds DESC LIMIT " + FULLTEXT_SEARCH_RESULTS_LIMIT);
            long delta = System.currentTimeMillis() - start;
            Log.d(TAG, "Found " + c.getCount() + " local results in " + delta + "ms. ");
            //no query should ever take this long.
            if (delta > 3000) {
                Log.w(TAG, "Warning: Results took too long, there's something wrong with the database, you might want to delete some data.");
            }

            List<SearchResult> results = new ArrayList<SearchResult>();
            /*
            Map<Integer, SearchEngine> searchEngines = SearchEngine.getSearchEngineMap();

            while (c.moveToNext()) {
                try {
                    String json = c.getString(0);

                    TorrentFileDB tfdb = JsonUtils.toObject(json, TorrentFileDB.class);

                    if (!searchEngines.get(tfdb.torrent.searchEngineID).isEnabled()) {
                        continue;
                    }

                    //results.add(new BittorrentLocalSearchResult(tfdb));
                    knownInfoHashes.add(tfdb.torrent.hash);
                } catch (Exception e) {
                    Log.e(TAG, "Error reading local search result", e);
                }
            }
            */

            Log.i(TAG, "Ended up with " + results.size() + " results");

            //addResults(results);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * The force parameter is very important here. Since we are in a constrained
     * environment we can't index huge torrents, but at the same time we want to force
     * the indexing in the already matched torrent's inner files. This is the reasoning
     * of pass this set with the relative paths.
     * 
     * @param sr
     * @param torrent
     * @param force
     */
    /*
    void indexTorrent(BittorrentWebSearchResult sr, TOTorrent torrent, Set<String> indexed) {
//        TorrentDB tdb = searchResultToTorrentDB(sr);
//        long now = System.currentTimeMillis();
//
//        TOTorrentFile[] files = torrent.getFiles();
//
//        for (int i = 0; i < files.length && i < MAX_TORRENT_FILES_TO_INDEX; i++) {
//            if (!indexed.contains(files[i].getRelativePath())) {
//                indexTorrentFile(now, files[i], tdb);
//                Thread.yield(); // try to play nice with others
//            }
//        }
    }*/
/*
    void indexTorrentFile(BittorrentWebSearchResult sr, TOTorrentFile file) {
//        TorrentDB tdb = searchResultToTorrentDB(sr);
//        long now = System.currentTimeMillis();
//        indexTorrentFile(now, file, tdb);
    }*/

    /*
    private TorrentDB searchResultToTorrentDB(BittorrentWebSearchResult sr) {
//        TorrentDB tdb = new TorrentDB();
//
//        tdb.creationTime = sr.getCreationTime();
//        tdb.fileName = sr.getFilename();
//        tdb.hash = sr.getHash();
//        tdb.seeds = sr.getSeeds();
//        tdb.size = sr.getSize();
//        tdb.torrentDetailsURL = sr.getDetailsUrl();
//        tdb.torrentURI = sr.getTorrentURI();
//        tdb.vendor = sr.getSource();

        return null;// tdb;
    }*/

    /*
    private void indexTorrentFile(long time, TOTorrentFile file, TorrentDB tdb) {
        TorrentFileDB tfdb = new TorrentFileDB();
        tfdb.relativePath = file.getRelativePath();
        tfdb.size = file.getLength();
        tfdb.torrent = tdb;

        String keywords = sanitize(tdb.fileName + " " + tfdb.relativePath).toLowerCase();
        keywords = addNormalizedTokens(keywords);
        //Log.d(TAG, "Keywords index: " + keywords);
        String json = JsonUtils.toJson(tfdb);

        insert(time, tdb.hash, tdb.fileName, tdb.seeds, tfdb.relativePath, keywords, json);

    }*/

    final static String sanitize(String str) {
        str = Html.fromHtml(str).toString();
        str = str.replaceAll("\\.torrent|www\\.|\\.com|\\.net|[\\\\\\/%_;\\-\\.\\(\\)\\[\\]\\n\\r�&~{}\\*@\\^'=!,�|#��]", " ");
        str = StringUtils.removeDoubleSpaces(str);
        //Log.d(TAG, "Sanitize result: " + str);
        return str;
    }

    final static String addNormalizedTokens(String str) {
        String[] tokens = str.split(" ");

        StringBuilder sb = new StringBuilder();

        for (String token : tokens) {
            String norm = Normalizer.normalize(token, Normalizer.Form.NFKD);
            norm = norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            if (!norm.equals(token)) {
                sb.append(" ");
                sb.append(norm);
            }
        }

        return str + sb.toString();
    }

    final static String normalizeTokens(String str) {
        String[] tokens = str.split(" ");

        StringBuilder sb = new StringBuilder();

        for (String token : tokens) {
            String norm = Normalizer.normalize(token, Normalizer.Form.NFKD);
            norm = norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            sb.append(norm);
            sb.append(" ");
        }

        return sb.toString().trim();
    }

    /*
    private boolean torrentIndexed(BittorrentWebSearchResult result) {
        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        try {
            c = cr.query(Torrents.Media.CONTENT_URI, new String[] { TorrentFilesColumns._ID }, "TORRENT_INFO_HASH LIKE ?", new String[] { result.getHash() }, null);
            return c.getCount() > 0;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }*/

    private void insert(long timestamp, String torrentInfoHash, String torrentFileName, int torrentSeeds, String relativePath, String keywords, String json) {
        synchronized (lockObj) {
            ContentResolver cr = context.getContentResolver();

            ContentValues cv = new ContentValues();

            cv.put(TorrentFilesColumns.TIMESTAMP, timestamp);
            cv.put(TorrentFilesColumns.TORRENT_INFO_HASH, torrentInfoHash);
            cv.put(TorrentFilesColumns.TORRENT_FILE_NAME, torrentFileName);
            cv.put(TorrentFilesColumns.TORRENT_SEEDS, torrentSeeds);
            cv.put(TorrentFilesColumns.RELATIVE_PATH, relativePath);
            cv.put(TorrentFilesColumns.KEYWORDS, keywords);
            cv.put(TorrentFilesColumns.JSON, json);

            cr.insert(Torrents.Media.CONTENT_URI, cv);
        }
    }

    private String buildFtsQuery(String query) {
        query = sanitize(query);

        Set<String> tokens = new HashSet<String>(Arrays.asList(query.toLowerCase().split(" ")));

        String fts = "";

        for (String token : tokens) {
            fts += token.toLowerCase() + " ";
        }

        return fts.trim();
    }
}