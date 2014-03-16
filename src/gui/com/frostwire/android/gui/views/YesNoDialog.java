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

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.frostwire.util.Ref;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class YesNoDialog extends DialogFragment {

    private static final String ID_KEY = "id";
    private static final String TITLE_KEY = "title";
    private static final String MESSAGE_KEY = "message";

    private YesNoDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (YesNoDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement YesNoDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        String id = args.getString(ID_KEY);
        int titleId = args.getInt(TITLE_KEY);
        int messageId = args.getInt(MESSAGE_KEY);

        Context ctx = getActivity();

        DialogListener yes = new DialogListener(listener, id, true);
        DialogListener no = new DialogListener(listener, id, false);

        return new AlertDialog.Builder(ctx).setMessage(messageId).setTitle(titleId).setPositiveButton(android.R.string.yes, yes).setNegativeButton(android.R.string.no, no).create();
    }

    public static YesNoDialog newInstance(String id, int titleId, int messageId) {
        YesNoDialog f = new YesNoDialog();

        Bundle args = new Bundle();
        args.putString(ID_KEY, id);
        args.putInt(TITLE_KEY, titleId);
        args.putInt(MESSAGE_KEY, messageId);
        f.setArguments(args);

        return f;
    }

    public interface YesNoDialogListener {

        public void onPositiveClick(String id);

        public void onNegativeClick(String id);
    }

    private static final class DialogListener implements OnClickListener {

        private final WeakReference<YesNoDialogListener> listenerRef;
        private final String id;
        private final boolean yes;

        public DialogListener(YesNoDialogListener listener, String id, boolean yes) {
            this.listenerRef = Ref.weak(listener);
            this.id = id;
            this.yes = yes;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (Ref.alive(listenerRef)) {
                if (yes) {
                    listenerRef.get().onPositiveClick(id);
                } else {
                    listenerRef.get().onNegativeClick(id);
                }
            }
        }
    }
}