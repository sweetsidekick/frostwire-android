package com.frostwire.android.gui.views;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

public class AbstractDialog extends DialogFragment {

    private final String tag;

    public AbstractDialog(String tag) {
        this.tag = tag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);
    }

    public void show(FragmentManager manager) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }

    @SuppressWarnings("unchecked")
    protected final <T extends View> T findView(Dialog dlg, int id) {
        return (T) dlg.findViewById(id);
    }
}
