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

import com.frostwire.android.R;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.android.util.FilenameUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class DesktopTransfer implements DownloadTransfer {

    private static final int STATUS_TRANSFERRING = 1;
    private static final int STATUS_COMPLETE = 2;
    private static final int STATUS_CANCELLED = 3;

    private static final int SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS = 1000;

    private final TransferManager manager;
    private final FileDescriptor fd;
    private final Date dateCreated;
    private final File savePath;

    private int status;
    public long bytesTransferred;
    public long averageSpeed; // in bytes

    // variables to keep the upload rate of this transfer
    private long speedMarkTimestamp;
    private long totalTransferredSinceLastSpeedStamp;

    DesktopTransfer(TransferManager manager, FileDescriptor fd) {
        this.manager = manager;
        this.fd = fd;
        this.dateCreated = new Date();

        this.savePath = new File(SystemUtils.getDesktopFilesirectory(), FilenameUtils.getName(fd.filePath));

        status = STATUS_TRANSFERRING;
    }

    @Override
    public String getDisplayName() {
        return FilenameUtils.getName(fd.filePath);
    }

    @Override
    public String getStatus() {
        return getStatusString(status);
    }

    @Override
    public int getProgress() {
        return isComplete() ? 100 : (int) ((bytesTransferred * 100) / fd.fileSize);
    }

    @Override
    public long getSize() {
        return fd.fileSize;
    }

    @Override
    public Date getDateCreated() {
        return dateCreated;
    }

    @Override
    public long getBytesReceived() {
        return bytesTransferred;
    }

    @Override
    public long getBytesSent() {
        return 0;
    }

    @Override
    public long getDownloadSpeed() {
        return averageSpeed;
    }

    @Override
    public long getUploadSpeed() {
        return 0;
    }

    @Override
    public long getETA() {
        long speed = getUploadSpeed();
        return speed > 0 ? (fd.fileSize - getBytesSent()) / speed : Long.MAX_VALUE;
    }

    @Override
    public boolean isComplete() {
        return bytesTransferred == fd.fileSize;
    }

    @Override
    public List<? extends TransferItem> getItems() {
        return Collections.emptyList();
    }

    @Override
    public void cancel() {
        if (status != STATUS_COMPLETE) {
            status = STATUS_CANCELLED;
        }
        manager.remove(this);
    }

    @Override
    public File getSavePath() {
        return savePath;
    }

    @Override
    public boolean isDownloading() {
        return status == STATUS_TRANSFERRING;
    }

    @Override
    public void cancel(boolean deleteData) {
        cancel();
    }

    public void addBytesTransferred(int n) {
        bytesTransferred += n;
        updateAverageTransferSpeed();
    }

    public void complete() {
        status = STATUS_COMPLETE;
        cancel();
    }

    public boolean isCanceled() {
        return status == STATUS_CANCELLED;
    }

    private String getStatusString(int status) {
        int resId;
        switch (status) {
        case STATUS_TRANSFERRING:
            resId = R.string.desktop_transfer_status_transferring;
            break;
        case STATUS_COMPLETE:
            resId = R.string.desktop_transfer_status_complete;
            break;
        case STATUS_CANCELLED:
            resId = R.string.desktop_transfer_status_cancelled;
            break;
        default:
            resId = R.string.desktop_transfer_status_unknown;
            break;
        }
        return String.valueOf(resId);
    }

    private void updateAverageTransferSpeed() {
        long now = System.currentTimeMillis();

        if (now - speedMarkTimestamp > SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS) {
            averageSpeed = ((bytesTransferred - totalTransferredSinceLastSpeedStamp) * 1000) / (now - speedMarkTimestamp);
            speedMarkTimestamp = now;
            totalTransferredSinceLastSpeedStamp = bytesTransferred;
        }
    }
}
