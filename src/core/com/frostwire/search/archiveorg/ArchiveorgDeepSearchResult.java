/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
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

package com.frostwire.search.archiveorg;

import java.util.Locale;

import com.frostwire.android.util.FilenameUtils;
import com.frostwire.search.CompleteSearchResult;
import com.frostwire.search.HttpSearchResult;
import com.frostwire.search.SearchResultLicence;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class ArchiveorgDeepSearchResult implements HttpSearchResult, CompleteSearchResult {

    private static final String DOWNLOAD_URL = "http://archive.org/download/%s/%s";

    private final ArchiveorgSearchResult sr;
    private final String filename;
    private final ArchiveorgFile file;

    private final String displayName;
    private final String downloadUrl;

    public ArchiveorgDeepSearchResult(ArchiveorgSearchResult sr, String filename, ArchiveorgFile file) {
        this.sr = sr;
        this.filename = filename;
        this.file = file;

        this.displayName = FilenameUtils.getBaseName(filename);
        this.downloadUrl = String.format(Locale.US, DOWNLOAD_URL, sr.getItem().identifier, filename);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDetailsUrl() {
        return sr.getDetailsUrl();
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long getSize() {
        try {
            return Long.parseLong(file.size);
        } catch (Throwable e) {
            return -1;
        }
    }

    @Override
    public String getSource() {
        return sr.getSource();
    }

    @Override
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public SearchResultLicence getLicence() {
        return sr.getLicence();
    }
}
