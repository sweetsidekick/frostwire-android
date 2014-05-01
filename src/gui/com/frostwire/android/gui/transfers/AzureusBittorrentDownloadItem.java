/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

import com.frostwire.vuze.VuzeFileInfo;

/**
 * @author gubatron
 * @author aldenml
 *
 */
final class AzureusBittorrentDownloadItem implements BittorrentDownloadItem {

    private final VuzeFileInfo info;
    private final String displayName;

    public AzureusBittorrentDownloadItem(VuzeFileInfo info) {
        this.info = info;
        this.displayName = FilenameUtils.getBaseName(info.getFilename());
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public File getSavePath() {
        return info.getFile();
    }

    @Override
    public int getProgress() {
        return isComplete() ? 100 : (int) ((info.getDownloaded() * 100) / info.getLength());
    }

    @Override
    public long getSize() {
        return info.getLength();
    }

    @Override
    public boolean isComplete() {
        return info.getDownloaded() == info.getLength();
    }
}
