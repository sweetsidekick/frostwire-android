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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.util.ByteArrayBuffer;

import android.os.SystemClock;
import android.util.Log;

import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.DesktopUploadRequestStatus;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.httpserver.Code;
import com.frostwire.android.httpserver.HttpExchange;
import com.frostwire.android.httpserver.HttpHandler;
import com.frostwire.android.util.StringUtils;
import com.frostwire.util.JsonUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
final class DesktopUploadRequestHandler implements HttpHandler {

    private static final String TAG = "FW.DesktopUploadRequestHandler";

    private static final int READ_BUFFER_SIZE = 4 * 1024;
    private static final int MAX_SECONDS_WAIT_AUTHORIZATION = 60;

    private final SessionManager sessionManager;

    public DesktopUploadRequestHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream os = null;

        try {

            DesktopUploadRequest dur = readPOST(exchange.getRequestBody());

            if (dur == null || StringUtils.isNullOrEmpty(dur.address, true) || StringUtils.isNullOrEmpty(dur.computerName, true) || sessionManager.hasDURPending()) {
                exchange.sendResponseHeaders(Code.HTTP_BAD_REQUEST, 0);
                return;
            }

            if (dur.files == null || dur.files.size() == 0) {
                exchange.sendResponseHeaders(Code.HTTP_BAD_REQUEST, 0);
                return;
            }

            for (FileDescriptor fd : dur.files) {
                if (StringUtils.isNullOrEmpty(fd.filePath, true) || fd.fileSize == 0) {
                    exchange.sendResponseHeaders(Code.HTTP_BAD_REQUEST, 0);
                    return;
                }
            }

            String token = sessionManager.addDUR(dur);
            if (token != null) {
                Engine.instance().getDesktopUploadManager().notifyRequest(token);
            } else {
                exchange.sendResponseHeaders(Code.HTTP_FORBIDDEN, 0);
                return;
            }

            if (waitForAccept(token)) {
                byte[] response = token.getBytes("UTF-8");

                exchange.sendResponseHeaders(Code.HTTP_OK, response.length);

                os = exchange.getResponseBody();

                os.write(response);
            } else {
                Log.d(TAG, "Request not accepted");
                exchange.sendResponseHeaders(Code.HTTP_FORBIDDEN, 0);
            }

        } catch (Throwable e) {
            Log.e(TAG, "Error processing desktop upload request", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Throwable e) {
                    // ignore
                }
            }
            exchange.close();
        }
    }

    private DesktopUploadRequest readPOST(InputStream is) throws IOException {
        DesktopUploadRequest request = null;

        try {
            ByteArrayBuffer arr = new ByteArrayBuffer(READ_BUFFER_SIZE);

            byte[] buff = new byte[READ_BUFFER_SIZE];
            int n;

            while ((n = is.read(buff, 0, buff.length)) != -1) {
                arr.append(buff, 0, n);
            }

            String json = new String(arr.toByteArray(), "UTF-8");
            request = JsonUtils.toObject(json, DesktopUploadRequest.class);

        } catch (Throwable e) {
            Log.e(TAG, "Error reading post from desktop upload request", e);
        } finally {
            try {
                is.close();
            } catch (Throwable e) {
                // ignore
            }
        }

        return request;
    }

    private boolean waitForAccept(String token) {
        int count = MAX_SECONDS_WAIT_AUTHORIZATION;

        while (count >= 0 && sessionManager.getDURStatus(token) != DesktopUploadRequestStatus.ACCEPTED) {
            SystemClock.sleep(1000);
            count--;

            if (sessionManager.getDURStatus(token) == DesktopUploadRequestStatus.REJECTED) {
                break;
            }

            sessionManager.refreshDUR(token);
        }

        return sessionManager.getDURStatus(token) == DesktopUploadRequestStatus.ACCEPTED;
    }
}
