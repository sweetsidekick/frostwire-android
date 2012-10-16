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

package com.frostwire.android.gui.transfers;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.os.SystemClock;
import android.util.Log;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.HttpFetcher;
import com.frostwire.android.core.HttpFetcherListener;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.SimpleZip;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.android.util.FilenameUtils;
import com.frostwire.android.util.concurrent.AbstractRunnable;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class HttpDownload implements DownloadTransfer {

    private static final String TAG = "FW.HttpDownload";

    private static final int STATUS_DOWNLOADING = 1;
    private static final int STATUS_COMPLETE = 2;
    private static final int STATUS_ERROR = 3;
    private static final int STATUS_CANCELLED = 4;
    private static final int STATUS_WAITING = 5;
    private static final int STATUS_UNCOMPRESSING = 6;

    private static final int SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS = 1000;

    private final TransferManager manager;
    private final HttpDownloadLink link;
    private final Date dateCreated;
    private final File savePath;

    private int status;
    private long bytesReceived;
    public long averageSpeed; // in bytes

    // variables to keep the download rate of file transfer
    private long speedMarkTimestamp;
    private long totalReceivedSinceLastSpeedStamp;

    HttpDownload(TransferManager manager, HttpDownloadLink link) {
        this.manager = manager;
        this.link = link;
        this.dateCreated = new Date();

        this.savePath = new File(SystemUtils.getTorrentDataDirectory(), link.getFileName());

        this.status = STATUS_DOWNLOADING;
    }

    public String getDisplayName() {
        return link.getDisplayName();
    }

    public String getStatus() {
        return getStatusString(status);
    }

    public int getProgress() {
        if (link.getSize() > 0) {
            return isComplete() ? 100 : (int) ((bytesReceived * 100) / link.getSize());
        } else {
            return 0;
        }
    }

    public long getSize() {
        return link.getSize();
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public long getBytesSent() {
        return 0;
    }

    public long getDownloadSpeed() {
        return averageSpeed;
    }

    public long getUploadSpeed() {
        return 0;
    }

    public long getETA() {
        if (link.getSize() > 0) {
            long speed = getDownloadSpeed();
            return speed > 0 ? (link.getSize() - getBytesReceived()) / speed : Long.MAX_VALUE;
        } else {
            return 0;
        }
    }

    public boolean isComplete() {
        if (bytesReceived > 0) {
            return bytesReceived == link.getSize();
        } else {
            return false;
        }
    }

    public boolean isDownloading() {
        return status == STATUS_DOWNLOADING;
    }

    public List<? extends TransferItem> getItems() {
        return Collections.emptyList();
    }

    public File getSavePath() {
        return savePath;
    }

    public void cancel() {
        cancel(false);
    }

    public void cancel(boolean deleteData) {
        if (status != STATUS_COMPLETE) {
            status = STATUS_CANCELLED;
        }
        if (status != STATUS_COMPLETE || deleteData) {
            cleanup();
        }
        manager.remove(this);
    }

    public void start() {
        start(0, 0);
    }

    /**
     * 
     * @param delay in seconds.
     * @param retry
     */
    private void start(final int delay, final int retry) {
        Engine.instance().getThreadPool().execute(new AbstractRunnable(getDisplayName()) {
            public void run() {
                try {
                    status = STATUS_WAITING;
                    SystemClock.sleep(delay * 1000);

                    status = STATUS_DOWNLOADING;
                    String uri = link.getUrl();
                    new HttpFetcher(uri).save(savePath, new DownloadListener(retry));
                    Librarian.instance().scan(savePath);
                } catch (Throwable e) {
                    error(e);
                }
            }
        });
    }

    private String getStatusString(int status) {
        int resId;
        switch (status) {
        case STATUS_DOWNLOADING:
            resId = R.string.peer_http_download_status_downloading;
            break;
        case STATUS_COMPLETE:
            resId = R.string.peer_http_download_status_complete;
            break;
        case STATUS_ERROR:
            resId = R.string.peer_http_download_status_error;
            break;
        case STATUS_CANCELLED:
            resId = R.string.peer_http_download_status_cancelled;
            break;
        case STATUS_WAITING:
            resId = R.string.peer_http_download_status_waiting;
            break;
        case STATUS_UNCOMPRESSING:
            resId = R.string.http_download_status_uncompressing;
            break;
        default:
            resId = R.string.peer_http_download_status_unknown;
            break;
        }
        return String.valueOf(resId);
    }

    private void updateAverageDownloadSpeed() {
        long now = System.currentTimeMillis();

        if (now - speedMarkTimestamp > SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS) {
            averageSpeed = ((bytesReceived - totalReceivedSinceLastSpeedStamp) * 1000) / (now - speedMarkTimestamp);
            speedMarkTimestamp = now;
            totalReceivedSinceLastSpeedStamp = bytesReceived;
        }
    }

    private void complete() {
        boolean success = true;
        String location = null;
        if (link.isCompressed()) {
            status = STATUS_UNCOMPRESSING;
            location = FilenameUtils.removeExtension(savePath.getAbsolutePath());
            success = SimpleZip.uncompress(savePath.getAbsolutePath(), location);
        }

        if (success) {
            status = STATUS_COMPLETE;

            manager.incrementDownloadsToReview();
            Engine.instance().notifyDownloadFinished(getDisplayName(), getSavePath());
            Librarian.instance().scan(link.isCompressed() ? new File(location) : savePath.getAbsoluteFile());
        } else {
            error(new Exception("Error"));
        }
    }

    private void error(Throwable e) {
        if (status != STATUS_CANCELLED) {
            Log.e(TAG, String.format("Error downloading url: %s", link.getUrl()), e);
            status = STATUS_ERROR;
            cleanup();
        }
    }

    private void cleanup() {
        try {
            savePath.delete();
        } catch (Throwable tr) {
            // ignore
        }
    }

    /**
     * Normalize the url to a valid OS name postfixed url.
     * 
     * @param url
     * @return
     */
    private String normalize(String url) {
        return url; // nothing for now since we are dealing with controlled http downloads
    }

    private final class DownloadListener implements HttpFetcherListener {

        private final int retry;

        public DownloadListener(int retry) {
            this.retry = retry;
        }

        public void onData(byte[] data, int length) {
            bytesReceived += length;
            updateAverageDownloadSpeed();

            if (status == STATUS_CANCELLED) {
                // ok, this is not the most elegant solution but it effectively breaks the
                // download logic flow.
                throw new RuntimeException("Invalid status, transfer cancelled");
            }
        }

        public void onSuccess(byte[] body) {
            complete();
        }

        public void onError(Throwable e, int statusCode, Map<String, String> headers) {
            try {
                if (statusCode == 503 && headers.containsKey("Retry-After") && retry < Constants.MAX_PEER_HTTP_DOWNLOAD_RETRIES) {
                    int delay = Integer.parseInt(headers.get("Retry-After"));
                    if (delay > 0) {
                        start(delay, retry + 1);
                    } else {
                        error(e);
                    }
                } else {
                    error(e);
                }
            } catch (Throwable tr) {
                error(tr);
            }
        }
    }
}
