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

package com.frostwire.android.gui.services;

import android.app.Service;
import android.content.Intent;

import com.frostwire.android.core.Constants;
import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.DesktopUploadRequestStatus;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.httpserver.SessionManager;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class DesktopUploadManager {

    private final Service service;
    private final SessionManager sessionManager;

    DesktopUploadManager(Service service, SessionManager sessionManager) {
        this.service = service;
        this.sessionManager = sessionManager;
    }

    public void notifyRequest(String token) {
        Intent i = new Intent(Constants.ACTION_DESKTOP_UPLOAD_REQUEST);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Constants.EXTRA_DESKTOP_UPLOAD_REQUEST_TOKEN, token);
        service.startActivity(i.setClass(service, MainActivity.class));
    }

    public DesktopUploadRequest getRequest(String token) {
        return sessionManager.getDUR(token);
    }

    public void authorizeRequest(String token) {
        sessionManager.updateDURStatus(token, DesktopUploadRequestStatus.ACCEPTED);
    }

    public void rejectRequest(String token) {
        sessionManager.updateDURStatus(token, DesktopUploadRequestStatus.REJECTED);
    }

    public void blockComputer(String token) {
        rejectRequest(token);
    }
}
