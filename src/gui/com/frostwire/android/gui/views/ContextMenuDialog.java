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

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class ContextMenuDialog extends DialogFragment {

    private List<ContextMenuItem> items;

    public ContextMenuDialog() {
    }

    public List<ContextMenuItem> getItems() {
        return items;
    }

    public void setItems(List<ContextMenuItem> items) {
        this.items = items;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();

        final ContextMenuAdapter adapter = new ContextMenuAdapter(LayoutInflater.from(context), items);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setAdapter(adapter, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ContextMenuItem item = adapter.getItem(which);
                item.onClick();
                dismiss();
            }
        });

        return builder.create();
    }
}
