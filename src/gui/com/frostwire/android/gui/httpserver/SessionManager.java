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

package com.frostwire.android.gui.httpserver;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import android.support.v4.util.LruCache;

import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.DesktopUploadRequestStatus;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SessionManager {

    private static final int MAX_DESKTOP_UPLOAD_REQUESTS = 1;
    private static final int DESKTIP_UPLOAD_REQUEST_UPDATE_TIMEOUT = 5000;

    private final LruCache<String, DesktopUploadRequest> durCache;

    public SessionManager() {
        this.durCache = new LruCache<String, DesktopUploadRequest>(MAX_DESKTOP_UPLOAD_REQUESTS);
    }

    public DesktopUploadRequest getDesktopUploadRequest(String token) {
        purgeDUROld();
        return durCache.get(token);
    }

    String addDUR(DesktopUploadRequest request) {
        purgeDUROld();
        if (durCache.size() == durCache.maxSize()) {
            return null; // not possible to accept more requests
        }

        String token = createDURToken();

        request.token = token;
        updateDURStatus(token, DesktopUploadRequestStatus.PENDING);

        return token;
    }

    DesktopUploadRequestStatus getDURStatus(String token) {
        purgeDUROld();
        DesktopUploadRequest dur = durCache.get(token);
        return dur != null ? dur.status : DesktopUploadRequestStatus.REJECTED;
    }

    void updateDURStatus(String token, DesktopUploadRequestStatus status) {
        purgeDUROld();
        DesktopUploadRequest dur = durCache.get(token);
        if (dur != null) {
            dur.status = status;
            dur.updateTimestamp = System.currentTimeMillis();
        }
    }

    boolean hasDURPending() {
        purgeDUROld();
        Map<String, DesktopUploadRequest> snapshot = durCache.snapshot();

        for (DesktopUploadRequest dur : snapshot.values()) {
            if (dur.status == DesktopUploadRequestStatus.PENDING) {
                return true;
            }
        }

        return false;
    }

    private String createDURToken() {
        return UUID.randomUUID().toString();
    }

    private void purgeDUROld() {
        Map<String, DesktopUploadRequest> snapshot = durCache.snapshot();

        long now = System.currentTimeMillis();
        for (Entry<String, DesktopUploadRequest> entry : snapshot.entrySet()) {
            if (now - entry.getValue().updateTimestamp > DESKTIP_UPLOAD_REQUEST_UPDATE_TIMEOUT) {
                durCache.remove(entry.getKey());
            }
        }
    }
}
