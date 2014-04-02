package com.frostwire.vuze;

public interface VuzeDownloadListener {

    public void stateChanged(VuzeDownloadManager dm, int state);
    
    public void downloadComplete(VuzeDownloadManager dm);
}
