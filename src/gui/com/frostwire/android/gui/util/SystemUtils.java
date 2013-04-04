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

package com.frostwire.android.gui.util;

import java.io.File;
import java.io.IOException;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.util.FileUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class SystemUtils {

    private static final String FROSTWIRE_FOLDER_NAME = "FrostWire";
    private static final String AUDIO_FOLDER_NAME = "Music";
    private static final String PICTURES_FOLDER_NAME = "Pictures";
    private static final String VIDEOS_FOLDER_NAME = "Videos";
    private static final String DOCUMENTS_FOLDER_NAME = "Documents";
    private static final String APPLICATIONS_FOLDER_NAME = "Applications";
    private static final String RINGTONES_FOLDER_NAME = "Ringtones";
    private static final String TORRENTS_FOLDER_NAME = "Torrents";
    private static final String TORRENT_DATA_FOLDER_NAME = "TorrentsData";
    private static final String DESKTOP_FILES_FOLDER_NAME = "DesktopFiles";
    private static final String TEMP_FOLDER_NAME = "Temp";
    private static final String AZUREUS_FOLDER_NAME = ".azureus";
    private static final String DEEPSCAN_FOLDER_NAME = ".deepscan";
    private static final String IMAGE_CACHE_FOLDER_NAME = ".image_cache";

    private static final String APPLICATION_NAME = "frostwire.apk";

    public static File getApplicationStorageDirectory() {
        String path = ConfigurationManager.instance().getString(Constants.PREF_KEY_STORAGE_PATH);
        File externalStorageDirectory = new File(path);

        return FileUtils.createFolder(externalStorageDirectory, FROSTWIRE_FOLDER_NAME);
    }

    public static File getAzureusDirectory() {
        return FileUtils.createFolder(getApplicationStorageDirectory(), AZUREUS_FOLDER_NAME);
    }

    public static File getTorrentsDirectory() {
        return FileUtils.createFolder(getApplicationStorageDirectory(), TORRENTS_FOLDER_NAME);
    }

    public static File getTorrentDataDirectory() {
        return FileUtils.createFolder(getApplicationStorageDirectory(), TORRENT_DATA_FOLDER_NAME);
    }

    public static File getDesktopFilesirectory() {
        return FileUtils.createFolder(getApplicationStorageDirectory(), DESKTOP_FILES_FOLDER_NAME);
    }

    public static File getTempDirectory() {
        File f = FileUtils.createFolder(getApplicationStorageDirectory(), TEMP_FOLDER_NAME);

        File nomedia = new File(f, ".nomedia");
        if (!nomedia.exists()) {
            try {
                nomedia.createNewFile();
            } catch (IOException e) {
                // unable to create nomedia file, ignore it for now
            }
        }

        return f;
    }

    public static File getDeepScanTorrentsDirectory() {
        return FileUtils.createFolder(getApplicationStorageDirectory(), DEEPSCAN_FOLDER_NAME);
    }

    public static File getSaveDirectory(byte fileType) {
        File parentFolder = getApplicationStorageDirectory();

        String folderName = null;

        switch (fileType) {
        case Constants.FILE_TYPE_AUDIO:
            folderName = AUDIO_FOLDER_NAME;
            break;
        case Constants.FILE_TYPE_PICTURES:
            folderName = PICTURES_FOLDER_NAME;
            break;
        case Constants.FILE_TYPE_VIDEOS:
            folderName = VIDEOS_FOLDER_NAME;
            break;
        case Constants.FILE_TYPE_DOCUMENTS:
            folderName = DOCUMENTS_FOLDER_NAME;
            break;
        case Constants.FILE_TYPE_APPLICATIONS:
            folderName = APPLICATIONS_FOLDER_NAME;
            break;
        case Constants.FILE_TYPE_RINGTONES:
            folderName = RINGTONES_FOLDER_NAME;
            break;
        default: // We will treat anything else like documents (unknown types)
            folderName = DOCUMENTS_FOLDER_NAME;
        }

        return FileUtils.createFolder(parentFolder, folderName);
    }

    public static File getUpdateInstallerPath() {
        return new File(SystemUtils.getSaveDirectory(Constants.FILE_TYPE_APPLICATIONS), APPLICATION_NAME);
    }

    public static File getImageCacheDirectory() {
        return FileUtils.createFolder(getApplicationStorageDirectory(), IMAGE_CACHE_FOLDER_NAME);
    }
}
