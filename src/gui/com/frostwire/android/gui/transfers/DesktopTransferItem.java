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

import org.apache.commons.io.FilenameUtils;

import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.util.SystemUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class DesktopTransferItem implements TransferItem {

    private static final int SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS = 1000;

    private final FileDescriptor fd;
    private final File savePath;

    public long bytesTransferred;
    public long averageSpeed; // in bytes

    // variables to keep the upload rate of this transfer
    private long speedMarkTimestamp;
    private long totalTransferredSinceLastSpeedStamp;

    private boolean failed;

    DesktopTransferItem(FileDescriptor fd) {
        this.fd = fd;
        this.savePath = new File(SystemUtils.getDesktopFilesirectory(), FilenameUtils.getName(fd.filePath));
    }

    public FileDescriptor getFD() {
        return fd;
    }

    public File getSavePath() {
        return savePath;
    }

    @Override
    public String getDisplayName() {
        return FilenameUtils.getName(fd.filePath);
    }

    @Override
    public int getProgress() {
        if (!failed) {
            return isComplete() ? 100 : (int) ((bytesTransferred * 100) / fd.fileSize);
        } else {
            return 0;
        }
    }

    @Override
    public long getSize() {
        return fd.fileSize;
    }

    @Override
    public boolean isComplete() {
        return bytesTransferred == fd.fileSize;
    }

    public void addBytesTransferred(int n) {
        bytesTransferred += n;
        updateAverageTransferSpeed();
    }

    public void setFailed() {
        failed = true;
        averageSpeed = 0;
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
