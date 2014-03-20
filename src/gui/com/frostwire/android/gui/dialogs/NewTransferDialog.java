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

package com.frostwire.android.gui.dialogs;

import java.io.Serializable;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.tasks.StartDownloadTask;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractDialog;
import com.frostwire.android.gui.views.ClickAdapter;
import com.frostwire.search.FileSearchResult;
import com.frostwire.search.SearchResult;
import com.frostwire.util.JsonUtils;
import com.frostwire.util.Ref;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class NewTransferDialog extends AbstractDialog {

    public static final String TAG = "new_transfer";

    private static final String SEARCH_RESULT_DATA_KEY = "search_result_data";
    private static final String HIDE_CHECK_SHOW_KEY = "hide_check_show";

    private Button buttonNo;
    private Button buttonYes;
    private CheckBox checkShow;

    public NewTransferDialog() {
        super(TAG, R.layout.dialog_new_transfer);
    }

    public static NewTransferDialog newInstance(FileSearchResult sr, boolean hideCheckShow) {
        NewTransferDialog f = new NewTransferDialog();

        Bundle args = new Bundle();
        args.putSerializable(SEARCH_RESULT_DATA_KEY, new SearchResultData(sr));
        args.putBoolean(HIDE_CHECK_SHOW_KEY, hideCheckShow);
        f.setArguments(args);

        return f;
    }

    @Override
    protected void initComponents(Dialog dlg, Bundle savedInstanceState) {
        Bundle args = getArguments();

        SearchResultData data = (SearchResultData) args.getSerializable(SEARCH_RESULT_DATA_KEY);
        boolean hideCheckShow = args.getBoolean(HIDE_CHECK_SHOW_KEY);

        dlg.setTitle(R.string.dialog_new_transfer_title);

        Context ctx = dlg.getContext();
        FileSearchResult sr = data.getSearchResult();

        String sizeStr = sr.getSize() > 0 ? UIUtils.getBytesInHuman(sr.getSize()) : ctx.getString(R.string.size_unknown);

        TextView textQuestion = findView(dlg, R.id.dialog_new_transfer_text);

        textQuestion.setText(dlg.getContext().getString(R.string.dialog_new_transfer_text_text, sr.getDisplayName(), sizeStr));

        DialogListener yes = new DialogListener(this, sr, true);
        DialogListener no = new DialogListener(this, sr, false);

        buttonYes = findView(dlg, R.id.dialog_new_transfer_button_yes);
        buttonYes.setOnClickListener(yes);

        buttonNo = findView(dlg, R.id.dialog_new_transfer_button_no);
        buttonNo.setOnClickListener(no);

        checkShow = findView(dlg, R.id.dialog_new_transfer_check_show);
        checkShow.setChecked(ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG));
        checkShow.setOnCheckedChangeListener(yes);

        if (hideCheckShow) {
            checkShow.setVisibility(View.GONE);
        }
    }

    private static final class DialogListener extends ClickAdapter<NewTransferDialog> {

        private final SearchResult sr;
        private final boolean positive;

        public DialogListener(NewTransferDialog owner, SearchResult sr, boolean positive) {
            super(owner);
            this.sr = sr;
            this.positive = positive;
        }

        @Override
        public void onClick(NewTransferDialog owner, View v) {
            if (positive && Ref.alive(owner.activityRef)) {
                StartDownloadTask.download(owner.activityRef.get(), sr, owner.getString(R.string.download_added_to_queue));
            }
            owner.dismiss();
        }

        @Override
        public void onCheckedChanged(NewTransferDialog owner, CompoundButton buttonView, boolean isChecked) {
            ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG, isChecked);
        }
    }

    /*
     * This is a very dangerous way to serialize the search result.
     */
    private static final class SearchResultData implements Serializable {

        private final String json;
        private final Class<? extends FileSearchResult> clazz;

        public SearchResultData(FileSearchResult sr) {
            this.json = JsonUtils.toJson(sr);
            this.clazz = sr.getClass();
        }

        public FileSearchResult getSearchResult() {
            return JsonUtils.toObject(json, clazz);
        }
    }
}
