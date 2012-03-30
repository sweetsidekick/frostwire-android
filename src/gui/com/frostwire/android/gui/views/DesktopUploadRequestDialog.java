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

package com.frostwire.android.gui.views;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.DesktopUploadRequest;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.util.UIUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class DesktopUploadRequestDialog extends Dialog {

    private Button buttonAccept;
    private Button buttonReject;
    private Button buttonBlock;

    private final DesktopUploadRequest request;
    private final OnDesktopUploadListener listener;

    public DesktopUploadRequestDialog(Context context, DesktopUploadRequest request, OnDesktopUploadListener listener) {
        super(context);
        this.request = request;
        this.listener = listener;
        initComponents();
    }

    @Override
    public void show() {
        super.show();
    }

    private void initComponents() {
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        setTitle(R.string.dialog_desktop_upload_request_title);
        setContentView(R.layout.dialog_desktop_upload_request);

        TextView text = (TextView) findViewById(R.id.dialog_desktop_upload_request_text);

        String filesStr = getContext().getResources().getQuantityString(R.plurals.num_files, request.files.size(), request.files.size());
        String totalFilesSizeStr = calculateTotalFilesSizeStr();

        text.setText(getContext().getString(R.string.dialog_desktop_upload_request_text_text, request.computerName, filesStr, totalFilesSizeStr));

        setCancelable(true);
        this.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dismiss();
                if (listener != null) {
                    listener.onResult(DesktopUploadRequestDialog.this, DesktopUploadRequestDialogResult.REJECT);
                }
            }
        });

        buttonAccept = (Button) findViewById(R.id.dialog_desktop_upload_request_button_accept);
        buttonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onResult(DesktopUploadRequestDialog.this, DesktopUploadRequestDialogResult.ACCEPT);
                }
            }
        });

        buttonReject = (Button) findViewById(R.id.dialog_desktop_upload_request_button_reject);
        buttonReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onResult(DesktopUploadRequestDialog.this, DesktopUploadRequestDialogResult.REJECT);
                }
            }
        });

        buttonBlock = (Button) findViewById(R.id.dialog_desktop_upload_request_button_block);
        buttonBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onResult(DesktopUploadRequestDialog.this, DesktopUploadRequestDialogResult.BLOCK);
                }
            }
        });
    }

    private String calculateTotalFilesSizeStr() {
        long total = 0;

        for (FileDescriptor fd : request.files) {
            total += fd.fileSize;
        }

        return UIUtils.getBytesInHuman(total);
    }

    public interface OnDesktopUploadListener {

        public void onResult(DesktopUploadRequestDialog dialog, DesktopUploadRequestDialogResult result);
    }
}
