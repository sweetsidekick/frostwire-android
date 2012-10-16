package com.frostwire.android.gui.transfers;

public class HttpDownloadLink {

    private String url;
    private long size;
    private String filename;
    private String displayName;
    private boolean compressed;

    public HttpDownloadLink(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFileName() {
        return filename;
    }

    public void setFileName(String name) {
        this.filename = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }
}
