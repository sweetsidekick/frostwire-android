/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.util.OSUtils;
import com.frostwire.android.gui.views.ClearableEditTextView.OnActionListener;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SearchInputView extends LinearLayout {

    private final SuggestionsAdapter adapter;

    private ClearableEditTextView textInput;
    
    private View dummyFocusView;

    private OnSearchListener onSearchListener;

    private int mediaTypeId;

    public SearchInputView(Context context, AttributeSet set) {
        super(context, set);

        this.adapter = new SuggestionsAdapter(context);
    }

    public OnSearchListener getOnSearchListener() {
        return onSearchListener;
    }

    public void setOnSearchListener(OnSearchListener listener) {
        this.onSearchListener = listener;
    }

    public boolean isEmpty() {
        return textInput.getText().length() == 0;
    }

    public String getText() {
        return textInput.getText();
    }

    public void updateHint(String newHint) {
        textInput.setHint(newHint);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View.inflate(getContext(), R.layout.view_searchinput, this);

        if (isInEditMode()) {
            return;
        }

        mediaTypeId = ConfigurationManager.instance().getLastMediaTypeFilter();

        textInput = (ClearableEditTextView) findViewById(R.id.view_search_input_text_input);
        textInput.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    startSearch(v);
                    return true;
                }
                return false;
            }
        });
        textInput.setOnActionListener(new OnActionListener() {
            public void onTextChanged(View v, String str) {
            }

            public void onClear(View v) {
                SearchInputView.this.onClear();
            }
        });
        textInput.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startSearch(textInput);
            }
        });
        textInput.setAdapter(adapter);

        updateHint(mediaTypeId);

        initRadioButton(R.id.view_search_input_radio_audio, Constants.FILE_TYPE_AUDIO);
        initRadioButton(R.id.view_search_input_radio_videos, Constants.FILE_TYPE_VIDEOS);
        initRadioButton(R.id.view_search_input_radio_pictures, Constants.FILE_TYPE_PICTURES);
        initRadioButton(R.id.view_search_input_radio_applications, Constants.FILE_TYPE_APPLICATIONS);
        initRadioButton(R.id.view_search_input_radio_documents, Constants.FILE_TYPE_DOCUMENTS);
        initRadioButton(R.id.view_search_input_radio_torrents, Constants.FILE_TYPE_TORRENTS);

        setFileTypeCountersVisible(false);
        
        dummyFocusView = findViewById(R.id.view_search_input_linearlayout_dummy);
    }

    private void startSearch(View v) {
        hideSoftInput(v);
        textInput.setListSelection(-1);
        textInput.dismissDropDown();
        adapter.discardLastResult();

        String query = textInput.getText().toString().trim();
        if (query.length() > 0) {
            onSearch(query, mediaTypeId);
        }
        
        dummyFocusView.requestFocus();
    }

    private void onSearch(String query, int mediaTypeId) {
        if (onSearchListener != null) {
            onSearchListener.onSearch(this, query, mediaTypeId);
        }
    }

    private void onMediaTypeSelected(int mediaTypeId) {
        if (onSearchListener != null) {
            onSearchListener.onMediaTypeSelected(this, mediaTypeId);
        }
    }

    private void onClear() {
        if (onSearchListener != null) {
            onSearchListener.onClear(this);
        }
    }

    private void hideSoftInput(View v) {
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void updateHint(int fileType) {
        String hint = getContext().getString(R.string.search_label) + " " + getContext().getString(R.string.files);

        if (OSUtils.isOUYA()) {
            String ouyaSearchHintPrefix = getContext().getResources().getString(R.string.ouya_search_hint_prefix);
            hint = ouyaSearchHintPrefix + " " + hint;
        }

        textInput.setHint(hint);
    }

    private RadioButton initRadioButton(int viewId, final byte fileType) {
        final RadioButton button = (RadioButton) findViewById(viewId);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                radioButtonFileTypeClick(fileType);
                UXStats.instance().log(UXAction.SEARCH_RESULT_FILE_TYPE_CLICK);
            }
        });

        if (mediaTypeId == fileType) {
            button.setChecked(true);
        }

        return button;
    }

    private void radioButtonFileTypeClick(final int mediaTypeId) {
        updateHint(mediaTypeId);
        onMediaTypeSelected(mediaTypeId);

        SearchInputView.this.mediaTypeId = mediaTypeId;
        ConfigurationManager.instance().setLastMediaTypeFilter(mediaTypeId);
    }

    public static interface OnSearchListener {

        public void onSearch(View v, String query, int mediaTypeId);

        public void onMediaTypeSelected(View v, int mediaTypeId);

        public void onClear(View v);
    }

    public void updateFileTypeCounter(byte fileType, int numFiles) {
        try {
            int radioId = Constants.FILE_TYPE_AUDIO;
            switch (fileType) {
            case Constants.FILE_TYPE_AUDIO:
                radioId = R.id.view_search_input_radio_audio;
                break;
            case Constants.FILE_TYPE_VIDEOS:
                radioId = R.id.view_search_input_radio_videos;
                break;
            case Constants.FILE_TYPE_PICTURES:
                radioId = R.id.view_search_input_radio_pictures;
                break;
            case Constants.FILE_TYPE_APPLICATIONS:
                radioId = R.id.view_search_input_radio_applications;
                break;
            case Constants.FILE_TYPE_DOCUMENTS:
                radioId = R.id.view_search_input_radio_documents;
                break;
            case Constants.FILE_TYPE_TORRENTS:
                radioId = R.id.view_search_input_radio_torrents;
                break;

            }

            RadioButton rButton = (RadioButton) findViewById(radioId);
            String numFilesStr = String.valueOf(numFiles);
            if (numFiles > 9999) {
                numFilesStr = "+1k";
            }
            rButton.setText(numFilesStr);
        } catch (Throwable e) {
            // NPE
        }
    }

    public void setFileTypeCountersVisible(boolean fileTypeCountersVisible) {
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.view_search_input_radiogroup_file_type);
        radioGroup.setVisibility(fileTypeCountersVisible ? View.VISIBLE : View.GONE);
    }
}