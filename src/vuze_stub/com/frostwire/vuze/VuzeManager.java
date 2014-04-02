package com.frostwire.vuze;

import java.util.List;

import com.frostwire.vuze.VuzeManager.LoadTorrentsListener;

public class VuzeManager {

    public static void setConfiguration(VuzeConfiguration conf) {
        // TODO Auto-generated method stub
        
    }

    public static VuzeManager getInstance() {
        return new VuzeManager();
    }

    public void resume() {
        // TODO Auto-generated method stub
        
    }

    public void pause() {
        // TODO Auto-generated method stub
        
    }

    public static interface LoadTorrentsListener {
        
        public void onLoad(List<VuzeDownloadManager> dms);
    }

    public long getDataReceiveRate() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getDataSendRate() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void loadTorrents(boolean stop, LoadTorrentsListener loadTorrentsListener, VuzeDownloadListener downloadListener) {
        // TODO Auto-generated method stub
        
    }

    public void setParameter(String key, long long1) {
        // TODO Auto-generated method stub
        
    }

    public VuzeDownloadManager find(byte[] decodeHex) {
        // TODO Auto-generated method stub
        return null;
    }
}
