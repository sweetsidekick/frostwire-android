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
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.search.SearchResult;
import com.frostwire.android.gui.util.UIUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class NewTransferDialog extends AbstractDialog {

    private Button buttonNo;
    private Button buttonYes;
    private CheckBox checkShow;

    private SearchResult searchResult;
    private OnYesNoListener listener;

    /** When opening .torrent files from outside you don't want to
     * give the user the option of not showing this dialog again,
     * and the question should be asked everytime to avoid starting
     * big transfers by mistake. */
    private boolean hideShowNextTimeOption;

    public NewTransferDialog() {
        super("new_transfer");
        this.hideShowNextTimeOption = false; // discuss the use of this variable with @gubatron
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(SearchResult searchResult) {
        this.searchResult = searchResult;
    }

    public OnYesNoListener getListener() {
        return listener;
    }

    public void setListener(OnYesNoListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dlg = new Dialog(getActivity());
        dlg.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        dlg.setTitle(R.string.dialog_new_transfer_title);
        dlg.setContentView(R.layout.dialog_new_transfer);
        setCancelable(true);

        TextView textQuestion = findView(dlg, R.id.dialog_new_transfer_text);

        String sizeString = dlg.getContext().getString(R.string.size_unknown);
        if (searchResult.getSize() > 0) {
            sizeString = UIUtils.getBytesInHuman(searchResult.getSize());
        }

        textQuestion.setText(dlg.getContext().getString(R.string.dialog_new_transfer_text_text, searchResult.getDisplayName(), sizeString));

        setCancelable(true);

        buttonNo = findView(dlg, R.id.dialog_new_transfer_button_no);
        buttonNo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onNo(NewTransferDialog.this);
                }
            }
        });

        buttonYes = findView(dlg, R.id.dialog_new_transfer_button_yes);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onYes(NewTransferDialog.this);
                }
            }
        });

        checkShow = findView(dlg, R.id.dialog_new_transfer_check_show);
        checkShow.setChecked(ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG));
        checkShow.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG, checkShow.isChecked());
            }
        });

        if (hideShowNextTimeOption) {
            checkShow.setVisibility(View.GONE);
        }

        return dlg;
    }

    public interface OnYesNoListener {

        public void onYes(NewTransferDialog dialog);

        public void onNo(NewTransferDialog dialog);
    }
}
