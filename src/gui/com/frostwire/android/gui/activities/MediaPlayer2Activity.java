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

package com.frostwire.android.gui.activities;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.services.NativeAndroidPlayer;
import com.frostwire.android.gui.util.MusicUtils;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractSwipeDetector;
import com.frostwire.android.gui.views.MediaControllerView;
import com.frostwire.android.util.StringUtils;
import com.google.ads.AdSize;
import com.google.ads.AdView;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class MediaPlayer2Activity extends AbstractActivity implements MediaControllerView.MediaPlayerControl {

    private static final String TAG = "FW.MediaPlayerActivity";

    private MediaControllerView mediaController;
    private MediaPlayer mediaPlayer;
    private FileDescriptor mediaFD;

    private BroadcastReceiver broadcastReceiver;

    private AdView adView;

    public MediaPlayer2Activity() {
        super(R.layout.activity_mediaplayer2, false, 0);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_MEDIA_PLAYER_STOPPED)) {
                    try {
                        finish();
                    } catch (Throwable e) {
                        // ignore
                    }
                } else if (intent.getAction().equals(Constants.ACTION_MEDIA_PLAYER_PLAY)) {
                    try {
                        initComponents();
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }
        };
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mediaController != null) {
            mediaController.sync();
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause();
            } catch (Throwable e) {
                Log.w(TAG, String.format("Review logic: %s", e.getMessage()));
            }
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.start();
            } catch (Throwable e) {
                Log.w(TAG, String.format("Review logic: %s", e.getMessage()));
            }
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            finish();
            Engine.instance().getMediaPlayer().stop();
        }
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (Throwable e) {
                // ignore
                return 0;
            }
        } else {
            return 0;
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (Throwable e) {
                // ignore
                return 0;
            }
        } else {
            return 0;
        }
    }

    public void seekTo(int i) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(i);
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    public boolean isPlaying() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.isPlaying();
            } catch (Throwable e) {
                // ignore
                return false;
            }
        } else {
            return false;
        }
    }

    public int getBufferPercentage() {
        return 0;
    }

    public boolean canPause() {
        return true;
    }

    public boolean canStop() {
        return true;
    }

    @Override
    protected void initComponents() {
        if (!(Engine.instance().getMediaPlayer() instanceof NativeAndroidPlayer)) {
            Log.e(TAG, "Only media player of type NativeAndroidPlayer is supported");
            return;
        }

        initGestures();

        mediaPlayer = ((NativeAndroidPlayer) Engine.instance().getMediaPlayer()).getMediaPlayer();
        mediaFD = Engine.instance().getMediaPlayer().getCurrentFD();

        if (mediaPlayer != null) {
            //mediaController = findView(R.id.activity_media_player_media_controller);
            //mediaController.setMediaPlayer(this);
        }

        if (mediaFD != null) {
            TextView artist = findView(R.id.activity_mediaplayer_artist);
            if (!StringUtils.isNullOrEmpty(mediaFD.artist, true)) {
                artist.setText(mediaFD.artist);
            }
            TextView title = findView(R.id.activity_mediaplayer_title);
            if (!StringUtils.isNullOrEmpty(mediaFD.title, true)) {
                title.setText(mediaFD.title);
            }
            TextView album = findView(R.id.activity_mediaplayer_album);
            if (!StringUtils.isNullOrEmpty(mediaFD.album, true)) {
                album.setText(mediaFD.album);
            }

            ImageView image = findView(R.id.activity_mediaplayer_artwork);
            Bitmap coverArt = readArtWork();
            if (coverArt != null) {
                image.setImageBitmap(coverArt);
            }
        } else {
            Engine.instance().getMediaPlayer().stop();
        }

        LinearLayout llayout = findView(R.id.activity_mediaplayer_adview_placeholder);
        adView = new AdView(this, AdSize.SMART_BANNER, Constants.ADMOB_PUBLISHER_ID);
        adView.setVisibility(View.GONE);
        llayout.addView(adView, 0);

        if (mediaFD != null) {
            UIUtils.supportFrostWire(adView, mediaFD.artist + " " + mediaFD.title + " " + mediaFD.album + " " + mediaFD.year);
        }
    }

    private void initGestures() {
        LinearLayout lowestLayout = findView(R.id.activity_mediaplayer_layout);
        lowestLayout.setOnTouchListener(new AbstractSwipeDetector() {
            @Override
            public void onLeftToRightSwipe() {
                Engine.instance().getMediaPlayer().playPrevious();
            }

            @Override
            public void onRightToLeftSwipe() {
                Engine.instance().getMediaPlayer().playNext();
            }

            @Override
            public boolean onMultiTouchEvent(View v, MotionEvent event) {
                Engine.instance().getMediaPlayer().togglePause();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Constants.ACTION_MEDIA_PLAYER_STOPPED);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_PLAY);
        registerReceiver(broadcastReceiver, filter);

        enableLock(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(broadcastReceiver);
        enableLock(true);
    }

    private Bitmap readArtWork() {
        Bitmap artwork = null;

        try {
            artwork = MusicUtils.getArtwork(this, mediaFD.id, -1);
            artwork = applyReflection(artwork);
        } catch (Throwable e) {
            Log.e(TAG, "Can't read the cover art for fd: " + mediaFD);
        }

        return artwork;
    }

    private void enableLock(boolean enable) {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);

        if (enable) {
            lock.reenableKeyguard();
        } else {
            lock.disableKeyguard();
        }
    }

    private Bitmap applyReflection(Bitmap bitmap) {

        //The gap we want between the reflection and the original image
        final int reflectionGap = 4;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        //This will not scale but will flip on the Y axis
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);

        //Create a Bitmap with the flip matix applied to it.
        //We only want the bottom half of the image
        Bitmap reflectionImage = Bitmap.createBitmap(bitmap, 0, height / 2, width, height / 2, matrix, false);

        //Create a new bitmap with same width but taller to fit reflection
        Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + height / 2), Config.ARGB_8888);

        //Create a new Canvas with the bitmap that's big enough for
        //the image plus gap plus reflection
        Canvas canvas = new Canvas(bitmapWithReflection);
        //Draw in the original image
        canvas.drawBitmap(bitmap, 0, 0, null);
        //Draw in the gap
        Paint deafaultPaint = new Paint();
        canvas.drawRect(0, height, width, height + reflectionGap, deafaultPaint);
        //Draw in the reflection
        canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

        //Create a shader that is a linear gradient that covers the reflection
        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0, bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);
        //Set the paint to use this shader (linear gradient)
        paint.setShader(shader);
        //Set the Transfer mode to be porter duff and destination in
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        //Draw a rectangle using the paint with our linear gradient
        canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

        return bitmapWithReflection;
    }
}