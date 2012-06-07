package com.frostwire.android.bittorrent.websearch;

import com.frostwire.android.core.AndroidHttpFetcher;
import com.frostwire.android.util.StringUtils;

public abstract class JsonSearchPerformer implements WebSearchPerformer {

    public final String fetchJson(String url) {
        AndroidHttpFetcher fetcher = new AndroidHttpFetcher(url);

        byte[] bytes = fetcher.fetch();

        return bytes != null ? StringUtils.getUTF8String(bytes) : null;
    }
}
