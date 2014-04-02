package com.frostwire.vuze;

public interface VuzeTorrentDownloadListener {

    public void onFinished(VuzeTorrentDownloader dl);
    
    public void onError(VuzeTorrentDownloader dl);
}
