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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.util.Log;

import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.DesktopUploadRequestStatus;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.transfers.DesktopTransfer;
import com.frostwire.android.gui.transfers.TransferManager;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.android.httpserver.Code;
import com.frostwire.android.httpserver.HttpExchange;
import com.frostwire.android.httpserver.HttpHandler;
import com.frostwire.android.util.FilenameUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
final class DesktopUploadHandler implements HttpHandler {

    private static final String TAG = "FW.DesktopUploadHandler";

    private final SessionManager sessionManager;

    public DesktopUploadHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String filePath = null;
        String token = null;

        try {

            List<NameValuePair> query = URLEncodedUtils.parse(exchange.getRequestURI(), "UTF-8");

            for (NameValuePair item : query) {
                if (item.getName().equals("filePath")) {
                    filePath = item.getValue();
                }
                if (item.getName().equals("token")) {
                    token = item.getValue();
                }
            }

            if (filePath == null || token == null) {
                sessionManager.updateDURStatus(token, DesktopUploadRequestStatus.REJECTED);
                exchange.sendResponseHeaders(Code.HTTP_BAD_REQUEST, 0);
                return;
            }

            if (!durAllowed(filePath, token, exchange.getRemoteAddress().getAddress().getHostAddress())) {
                sessionManager.updateDURStatus(token, DesktopUploadRequestStatus.REJECTED);
                exchange.sendResponseHeaders(Code.HTTP_FORBIDDEN, 0);
                return;
            }

            if (!readFile(exchange.getRequestBody(), filePath, token)) {
                sessionManager.updateDURStatus(token, DesktopUploadRequestStatus.REJECTED);
                exchange.sendResponseHeaders(Code.HTTP_FORBIDDEN, 0);
                return;
            }

            exchange.sendResponseHeaders(Code.HTTP_OK, 0);

        } catch (Throwable e) {
            Log.e(TAG, String.format("Error receiving file from desktop: fileName=%s, token=%s", filePath, token), e);
        } finally {
            exchange.close();
        }
    }

    private FileDescriptor findFD(DesktopUploadRequest dur, String filePath) {
        for (FileDescriptor fd : dur.files) {
            if (fd.filePath.equals(filePath)) {
                return fd;
            }
        }

        return null;
    }

    private boolean durAllowed(String fileName, String token, String address) {
        DesktopUploadRequest dur = sessionManager.getDUR(token);

        if (dur == null || dur.status != DesktopUploadRequestStatus.ACCEPTED) {
            return false;
        }

        if (!dur.address.equals(address)) {
            return false;
        }

        if (findFD(dur, fileName) == null) {
            return false;
        }

        return true;
    }

    private boolean readFile(InputStream is, String filePath, String token) {
        String fileName = FilenameUtils.getName(filePath);
        FileOutputStream fos = null;

        File file = null;
        DesktopTransfer transfer = null;

        try {

            file = new File(SystemUtils.getTempDirectory(), fileName);

            fos = new FileOutputStream(file);

            DesktopUploadRequest dur = sessionManager.getDUR(token);
            FileDescriptor fd = findFD(dur, filePath);

            transfer = TransferManager.instance().desktopTransfer(fd);

            byte[] buffer = new byte[4 * 1024];
            int n;

            while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                fos.write(buffer, 0, n);
                if (transfer.isCanceled()) {
                    sessionManager.updateDURStatus(token, DesktopUploadRequestStatus.REJECTED);
                    file.delete();
                    return false;
                } else {
                    transfer.addBytesTransferred(n);
                    sessionManager.updateDURStatus(token, DesktopUploadRequestStatus.UPLOADING);
                }
            }

            sessionManager.updateDURStatus(token, DesktopUploadRequestStatus.ACCEPTED);

            File finalFile = new File(SystemUtils.getDesktopFilesirectory(), file.getName());

            if (file.renameTo(finalFile)) {
                Librarian.instance().scan(finalFile.getAbsoluteFile());
                return true;
            } else {
                file.delete();
            }

        } catch (Throwable e) {
            Log.e(TAG, String.format("Error saving file: fileName=%s, token=%s", fileName, token), e);

            if (file != null) {
                file.delete();
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Throwable e) {
                    // ignore
                }
            }
            try {
                is.close();
            } catch (Throwable e) {
                // ignore
            }

            if (transfer != null) {
                transfer.complete();
            }
        }

        return false;
    }
}
