package com.frostwire.android.gui.adapters;

import java.util.Arrays;

import android.content.Context;
import android.widget.ArrayAdapter;

public class SearchInputFileTypesAdapter extends ArrayAdapter<String> {

    public SearchInputFileTypesAdapter(Context context) {
        super(context, android.R.layout.simple_spinner_item, Arrays.asList("a", "b"));
    }
}
