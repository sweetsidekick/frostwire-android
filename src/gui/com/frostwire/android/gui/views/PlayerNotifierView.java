package com.frostwire.android.gui.views;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.core.player.CoreMediaPlayer;
import com.frostwire.android.gui.activities.MediaPlayerActivity;
import com.frostwire.android.gui.services.Engine;

public class PlayerNotifierView extends LinearLayout implements Refreshable {

    private String lastStatusShown;
    private TextView statusText;
    private LinearLayout statusContainer;

    private TranslateAnimation fromRightAnimation;

    private TranslateAnimation showNotifierAnimation;
    private TranslateAnimation hideNotifierAnimation;

    public PlayerNotifierView(Context context, AttributeSet set) {
        super(context, set);

        initAnimations();
    }

    private void initAnimations() {
        fromRightAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        fromRightAnimation.setDuration(500);
        fromRightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        showNotifierAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        showNotifierAnimation.setDuration(300);
        showNotifierAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        hideNotifierAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        hideNotifierAnimation.setDuration(300);
        hideNotifierAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View.inflate(getContext(), R.layout.view_player_notifier, this);
        statusText = (TextView) findViewById(R.id.view_player_notifier_status);
        statusContainer = (LinearLayout) findViewById(R.id.view_player_notifier_status_container);
        
        refreshPlayerStateIndicator();
    }

    private void refreshPlayerStateIndicator() {
        CoreMediaPlayer mediaPlayer = Engine.instance().getMediaPlayer();
        if (mediaPlayer == null) {
            return;
        }

        TextView isPlayingText = (TextView) findViewById(R.id.view_player_notifier_playing);
        
        if (!mediaPlayer.isPlaying()) {
            isPlayingText.setText(R.string.paused);
        } else {
            isPlayingText.setText(R.string.playing);
        }
    }

    @Override
    public void refresh() {
        FileDescriptor fd = Engine.instance().getMediaPlayer().getCurrentFD();
        String status = "";
        
        refreshPlayerStateIndicator();

        if (fd != null) {
            status = fd.artist + " - " + fd.title;

            if (getVisibility() == View.GONE) {
                setVisibility(View.VISIBLE);
                startAnimation(showNotifierAnimation);
            }
        } else {
            if (getVisibility() == View.VISIBLE) {
                startAnimation(hideNotifierAnimation);
                setVisibility(View.GONE);
            }
        }

        if (!status.equals(lastStatusShown)) {
            statusText.setText(status);
            lastStatusShown = status;
            statusContainer.startAnimation(fromRightAnimation);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Engine.instance().getMediaPlayer().getCurrentFD() != null) {
            Intent i = new Intent(getContext(), MediaPlayerActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
        }
        return true;
    }
}