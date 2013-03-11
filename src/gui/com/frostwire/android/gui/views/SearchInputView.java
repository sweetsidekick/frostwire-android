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

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.MediaType;
import com.frostwire.android.gui.search.SuggestionsAdapter;
import com.frostwire.android.gui.util.OSUtils;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.ClearableEditTextView.OnActionListener;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SearchInputView extends LinearLayout {

    private final SuggestionsAdapter adapter;

    private ImageButton buttonMediaType;
    private ClearableEditTextView textInput;

    private OnSearchListener onSearchListener;

    private int mediaTypeId;
    private PopupWindow popup;

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

        buttonMediaType = (ImageButton) findViewById(R.id.view_search_input_button_mediatype);
        buttonMediaType.setImageResource(getDrawableId(mediaTypeId));
        buttonMediaType.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonMediaType_onClick(v);
            }
        });

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
    }

    @Override
    protected void onDetachedFromWindow() {
        hidePopup();
        super.onDetachedFromWindow();
    }

    protected void buttonMediaType_onClick(View v) {
        showPopup(v);
    }

    private void showPopup(View v) {
        hideSoftInput(v);
        popup = newPopup();
        popup.showAsDropDown(this, 20, 0);
    }

    private void hidePopup() {
        if (popup != null) {
            popup.dismiss();
            popup = null;
        }
    }

    private PopupWindow newPopup() {
        final PopupWindow popup = new PopupWindow(getContext());

        popup.setTouchInterceptor(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    hidePopup();
                    return true;
                }

                return false;
            }
        });

        popup.setTouchable(true);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(R.style.Animations_GrowFromLeft);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.view_searchinput_menu_mediatype, null);
        view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        view.setLayoutParams(new LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));

        setupMenuItem(view, R.id.view_searchinput_menu_mediatype_audio, MediaType.getAudioMediaType().getId());
        setupMenuItem(view, R.id.view_searchinput_menu_mediatype_video, MediaType.getVideoMediaType().getId());
        setupMenuItem(view, R.id.view_searchinput_menu_mediatype_images, MediaType.getImageMediaType().getId());
        setupMenuItem(view, R.id.view_searchinput_menu_mediatype_applications, MediaType.getApplicationsMediaType().getId());
        setupMenuItem(view, R.id.view_searchinput_menu_mediatype_documents, MediaType.getDocumentMediaType().getId());
        setupMenuItem(view, R.id.view_searchinput_menu_mediatype_torrents, MediaType.getTorrentMediaType().getId());

        popup.setContentView(view);
        popup.setBackgroundDrawable(getResources().getDrawable(R.drawable.searchinput_menu_mediatype_background));
        popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        if (OSUtils.isKindleFire()) {
            popup.setHeight(450);
        }

        return popup;
    }

    private void setupMenuItem(View view, int id, final int mediaTypeId) {
        final Button b = (Button) view.findViewById(id);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateHint(mediaTypeId);
                onMediaTypeSelected(mediaTypeId);

                buttonMediaType.setImageResource(getDrawableId(mediaTypeId));
                SearchInputView.this.mediaTypeId = mediaTypeId;
                ConfigurationManager.instance().setLastMediaTypeFilter(mediaTypeId);

                hidePopup();
            }
        });
    }

    private int getDrawableId(int mediaTypeId) {
        if (MediaType.getApplicationsMediaType().getId() == mediaTypeId) {
            return R.drawable.browse_peer_application_icon_selector_on;
        } else if (MediaType.getAudioMediaType().getId() == mediaTypeId) {
            return R.drawable.browse_peer_audio_icon_selector_on;
        } else if (MediaType.getDocumentMediaType().getId() == mediaTypeId) {
            return R.drawable.browse_peer_document_icon_selector_on;
        } else if (MediaType.getImageMediaType().getId() == mediaTypeId) {
            return R.drawable.browse_peer_picture_icon_selector_on;
        } else if (MediaType.getVideoMediaType().getId() == mediaTypeId) {
            return R.drawable.browse_peer_video_icon_selector_on;
        } else if (MediaType.getTorrentMediaType().getId() == mediaTypeId) {
            return R.drawable.browse_peer_torrent_icon_selector_on;
        } else {
            return R.drawable.question_mark;
        }
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
        String hint = getContext().getString(R.string.search_label) + " ";
        hint += UIUtils.getFileTypeAsString(getContext().getResources(), (byte) fileType);
        textInput.setHint(hint);
    }

    public static interface OnSearchListener {

        public void onSearch(View v, String query, int mediaTypeId);

        public void onMediaTypeSelected(View v, int mediaTypeId);

        public void onClear(View v);
    }
}