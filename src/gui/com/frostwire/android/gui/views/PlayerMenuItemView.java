package com.frostwire.android.gui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.MusicUtils;

public class PlayerMenuItemView extends LinearLayout {

    private ImageView imageThumbnail;
    private TextView textTitle;
    private TextView textArtist;

    public PlayerMenuItemView(Context context, AttributeSet set) {
        super(context, set);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View.inflate(getContext(), R.layout.view_player_menu_item, this);

        imageThumbnail = (ImageView) findViewById(R.id.view_player_menu_item_thumbnail);
        textTitle = (TextView) findViewById(R.id.view_player_menu_item_title);
        textArtist = (TextView) findViewById(R.id.view_player_menu_item_artist);

        FileDescriptor fd = Engine.instance().getMediaPlayer().getCurrentFD();

        if (fd != null) {
            if (getVisibility() == View.GONE) {
                setVisibility(View.VISIBLE);

                imageThumbnail.setImageBitmap(MusicUtils.getArtwork(getContext(), fd.id, -1));
                textTitle.setText(fd.title);
                textArtist.setText(fd.artist);
            }
        } else {
            if (getVisibility() == View.VISIBLE) {
                setVisibility(View.GONE);

                imageThumbnail.setImageBitmap(null);
                textTitle.setText("");
                textArtist.setText("");
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        onTouchEvent(ev);
        return false;
    }
}