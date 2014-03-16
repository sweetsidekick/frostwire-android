/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ShareIndicationDialog extends AbstractDialog {

    public ShareIndicationDialog() {
        super("share_indication");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dlg = new Dialog(getActivity());
        dlg.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(R.layout.dialog_share_indication);
        setCancelable(true);

        final CheckBox checkShow = findView(dlg, R.id.dialog_share_indicator_check_show);
        checkShow.setChecked(ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_SHARE_INDICATION));

        Button buttonDone = findView(dlg, R.id.dialog_share_indicator_button_done);
        buttonDone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismiss();
                ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_SHOW_SHARE_INDICATION, checkShow.isChecked());
            }
        });

        return dlg;
    }
}
