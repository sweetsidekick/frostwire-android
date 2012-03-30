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
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import com.frostwire.android.R;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class DesktopUploadRequestDialog extends Dialog {

    private Button buttonNo;
    private Button buttonYes;

    private final OnDesktopUploadListener listener;

    public DesktopUploadRequestDialog(Context context, OnDesktopUploadListener listener) {
        super(context);
        this.listener = listener;
        initComponents();
    }

    @Override
    public void show() {
        super.show();
    }

    private void initComponents() {
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        setTitle(R.string.dialog_new_transfer_title);
        setContentView(R.layout.dialog_new_transfer);

        TextView textQuestion = (TextView) findViewById(R.id.dialog_new_transfer_text);

        String sizeString = getContext().getString(R.string.size_unknown);

        textQuestion.setText(getContext().getString(R.string.dialog_new_transfer_text_text, "title", sizeString));

        setCancelable(true);

        buttonNo = (Button) findViewById(R.id.dialog_new_transfer_button_no);
        buttonNo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onResult(DesktopUploadRequestDialog.this, DesktopUploadRequestDialogResult.REJECT);
                }
            }
        });

        buttonYes = (Button) findViewById(R.id.dialog_new_transfer_button_yes);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public interface OnDesktopUploadListener {

        public void onResult(DesktopUploadRequestDialog dialog, DesktopUploadRequestDialogResult result);
    }
}
