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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.apache.commons.io.IOUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.util.Ref;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class TOS extends DialogFragment {

    public static TOS newInstance() {
        TOS f = new TOS();

        f.setCancelable(true);

        return f;
    }

    private static String readTOS(Context context) {
        InputStream in = context.getResources().openRawResource(R.raw.tos);
        try {
            return IOUtils.toString(in);
        } catch (IOException e) {
            throw new RuntimeException("Missing TOS resource", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();

        DialogListener yes = new DialogListener(ctx instanceof TOSActivity ? (TOSActivity) ctx : null);
        DialogListener cancel = new DialogListener(null);

        return new AlertDialog.Builder(ctx).setTitle(R.string.tos_title).setMessage(readTOS(ctx)).setPositiveButton(R.string.tos_accept, yes).setNegativeButton(R.string.tos_refuse, cancel).create();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        exit();
    }

    private static void exit() {
        System.exit(1); // drastic action
    }

    public interface TOSActivity {
        public void onTOSAccept();
    }

    private static final class DialogListener implements OnClickListener {

        private final WeakReference<TOSActivity> activityRef;

        public DialogListener(TOSActivity activity) {
            this.activityRef = Ref.weak(activity);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (Ref.alive(activityRef)) {
                ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED, true);
                activityRef.get().onTOSAccept();
            } else {
                exit();
            }
        }
    }
}