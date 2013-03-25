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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.frostwire.android.R;
import com.frostwire.android.core.DesktopUploadRequest;
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

    private final TransferManager manager;
    private final DesktopUploadRequest dur;
    private final FileDescriptor fd;
    private final Date dateCreated;

    private final File savePath;
    private final List<DesktopTransferItem> items;

    private int status;
    private long lastSpeed;

    DesktopTransfer(TransferManager manager, DesktopUploadRequest dur, FileDescriptor fd) {
        this.manager = manager;
        this.dur = dur;
        this.fd = fd;
        this.dateCreated = new Date();

        this.savePath = new File(SystemUtils.getDesktopFilesirectory(), FilenameUtils.getName(fd.filePath));
        this.items = new ArrayList<DesktopTransferItem>();

        addFileDescriptor(fd);

        status = STATUS_TRANSFERRING;
    }

    public DesktopUploadRequest getDUR() {
        return dur;
    }

    public DesktopTransferItem getItem(FileDescriptor fd) {
        for (DesktopTransferItem item : items) {
            if (item.getFD().filePath.equals(fd.filePath)) {
                return item;
            }
        }

        return null;
    }

    public DesktopTransferItem addFileDescriptor(FileDescriptor fd) {
        DesktopTransferItem item = new DesktopTransferItem(fd);
        items.add(item);
        return item;
    }

    @Override
    public String getDisplayName() {
        if (items.size() == 1) {
            return FilenameUtils.getName(fd.filePath);
        } else {
            return dur.computerName;
        }
    }

    @Override
    public String getStatus() {
        return getStatusString(status);
    }

    @Override
    public int getProgress() {
        int progress = 0;

        if (isComplete()) {
            progress = 100;
        } else {
            long bytesTransferred = getBytesReceived();
            long totalSize = getSize();

            progress = bytesTransferred == totalSize ? 100 : (int) ((bytesTransferred * 100) / totalSize);
        }

        if (progress == 100) {
            complete();
        }

        return progress;
    }

    @Override
    public long getSize() {
        int totalSize = 0;

        for (FileDescriptor fd : dur.files) {
            totalSize += fd.fileSize;
        }

        return totalSize;
    }

    @Override
    public Date getDateCreated() {
        return dateCreated;
    }

    @Override
    public long getBytesReceived() {
        long bytesTransferred = 0;

        for (DesktopTransferItem item : items) {
            bytesTransferred += item.bytesTransferred;
        }

        return bytesTransferred;
    }

    @Override
    public long getBytesSent() {
        return 0;
    }

    @Override
    public long getDownloadSpeed() {
        if (isComplete()) {
            return 0;
        } else {
            long speed = 0;
            int n = 0;

            for (DesktopTransferItem item : items) {
                if (!item.isComplete() && item.averageSpeed > 0) {
                    speed += item.averageSpeed;
                    n++;
                }
            }

            if (n > 0) {
                lastSpeed = speed / n;
            }

            return lastSpeed;
        }
    }

    @Override
    public long getUploadSpeed() {
        return 0;
    }

    @Override
    public long getETA() {
        long speed = getDownloadSpeed();
        return speed > 0 ? (getSize() - getBytesReceived()) / speed : Long.MAX_VALUE;
    }

    @Override
    public boolean isComplete() {
        return getBytesReceived() == getSize();
    }

    @Override
    public List<? extends TransferItem> getItems() {
        if (items.size() > 1) {
            return items;
        } else {
            return Collections.emptyList();
        }
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

    public void complete() {
        status = STATUS_COMPLETE;
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
    
    @Override
    public String getDetailsUrl() {
        return null;
    }
}
