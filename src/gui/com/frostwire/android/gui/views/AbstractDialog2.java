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
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public abstract class AbstractDialog2 extends DialogFragment {

    private final String tag;
    private final int layoutResId;

    public AbstractDialog2(String tag, int layoutResId) {
        if (layoutResId == 0) {
            throw new RuntimeException("Resource id can't be 0");
        }

        this.tag = tag;
        this.layoutResId = layoutResId;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dlg = super.onCreateDialog(savedInstanceState);

        setContentView(dlg, layoutResId);
        initComponents(dlg, savedInstanceState);

        return dlg;
    }

    public void show(FragmentManager manager) {
        super.show(manager, tag);
    }

    protected void setContentView(Dialog dlg, int layoutResId) {
        dlg.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        dlg.setContentView(layoutResId);
    }

    protected abstract void initComponents(Dialog dlg, Bundle savedInstanceState);

    @SuppressWarnings("unchecked")
    protected final <T extends View> T findView(Dialog dlg, int id) {
        return (T) dlg.findViewById(id);
    }
}
