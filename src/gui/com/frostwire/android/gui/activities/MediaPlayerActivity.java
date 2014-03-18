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

package com.frostwire.android.gui.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.core.player.CoreMediaPlayer;
import com.frostwire.android.core.player.Playlist;
import com.frostwire.android.core.player.PlaylistItem;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.services.NativeAndroidPlayer;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractActivity;
import com.frostwire.android.gui.views.AbstractSwipeDetector;
import com.frostwire.android.gui.views.ContextMenuDialog;
import com.frostwire.android.gui.views.ContextMenuItem;
import com.frostwire.android.gui.views.ImageLoader;
import com.frostwire.android.gui.views.MediaPlayerControl;
import com.frostwire.android.util.StringUtils;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class MediaPlayerActivity extends AbstractActivity implements MediaPlayerControl {

    private static final String TAG = "FW.MediaPlayerActivity";

    private CoreMediaPlayer mediaPlayer;
    private FileDescriptor mediaFD;

    private BroadcastReceiver broadcastReceiver;

    private ImageButton buttonBack;
    private ImageButton buttonMenu;

    public MediaPlayerActivity() {
        super(R.layout.activity_mediaplayer);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String action = intent.getAction();

                    if (action != null) {
                        if (action.equals(Constants.ACTION_MEDIA_PLAYER_STOPPED)) {
                            try {
                                finish();
                            } catch (Throwable e) {
                                // ignore
                            }
                        } else if (action.equals(Constants.ACTION_MEDIA_PLAYER_PLAY) || action.equals(Constants.ACTION_MEDIA_PLAYER_PAUSED)) {
                            try {
                                initComponents(null);
                            } catch (Throwable e) {
                                // ignore
                            }
                        }
                    }
                }
            }
        };
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        sync();
    }

    public void pause() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.togglePause();
                UIUtils.showShortMessage(this, getString(R.string.player_paused_press_and_hold_to_stop));
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
            mediaPlayer.stop();
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
    protected void onCreate(Bundle savedInstance) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstance);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {

        if (!(Engine.instance().getMediaPlayer() instanceof NativeAndroidPlayer)) {
            Log.e(TAG, "Only media playerControl of type NativeAndroidPlayer is supported");
            return;
        }

        initGestures();

        mediaPlayer = (NativeAndroidPlayer) Engine.instance().getMediaPlayer();
        mediaFD = mediaPlayer.getCurrentFD();

        if (mediaPlayer != null) {
            setMediaPlayerControl(this);
        }

        if (mediaFD != null) {
            refreshUIData();
        } else {
            mediaPlayer.stop();
        }

        buttonBack = findView(R.id.activity_mediaplayer_button_back);
        buttonBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        buttonMenu = findView(R.id.activity_mediaplayer_button_menu);
        buttonMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayerContextMenu();
            }
        });

        // media playerControl controls
        buttonPrevious = (ImageButton) findViewById(R.id.activity_mediaplayer_button_previous);
        if (buttonPrevious != null) {
            buttonPrevious.setOnClickListener(previousListener);
        }

        buttonPause = (ImageButton) findViewById(R.id.activity_mediaplayer_button_play);
        if (buttonPause != null) {
            buttonPause.requestFocus();
            buttonPause.setOnClickListener(pauseListener);
            buttonPause.setOnLongClickListener(stopListener);
        }

        buttonNext = (ImageButton) findViewById(R.id.activity_mediaplayer_button_next);
        if (buttonNext != null) {
            buttonNext.setOnClickListener(nextListener);
        }

        progress = (SeekBar) findViewById(R.id.view_media_controller_progress);
        if (progress != null) {
            if (progress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) progress;
                seeker.setOnSeekBarChangeListener(seekListener);
            }
            progress.setMax(1000);
        }

        endTime = (TextView) findViewById(R.id.view_media_controller_time_end);
        currentTime = (TextView) findViewById(R.id.view_media_controller_time_current);
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        if (mediaFD != null) {
            UIUtils.initSupportFrostWire(this, R.id.activity_mediaplayer_donations_view_placeholder);
        }
    }

    private void refreshUIData() {
        //if for some weird reason this happens
        if (mediaFD == null) {
            return;
        }

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

        ImageView artworkImageView = findView(R.id.activity_mediaplayer_artwork);
        ImageLoader imageLoader = ImageLoader.getDefault();
        Drawable defaultArtWork = getResources().getDrawable(R.drawable.artwork_default);
        imageLoader.displayImage(mediaFD, artworkImageView, defaultArtWork);
    }

    private void initGestures() {
        findView(R.id.activity_mediaplayer_artwork).setOnTouchListener(new AbstractSwipeDetector() {
            @Override
            public void onLeftToRightSwipe() {
                playPrevious();
                UXStats.instance().log(UXAction.PLAYER_GESTURE_SWIPE_SONG);
            }

            @Override
            public void onRightToLeftSwipe() {
                playNext();
                UXStats.instance().log(UXAction.PLAYER_GESTURE_SWIPE_SONG);
            }

            @Override
            public boolean onMultiTouchEvent(View v, MotionEvent event) {
                pause();
                sync();
                UXStats.instance().log(UXAction.PLAYER_GESTURE_PAUSE_RESUME);
                return true;
            }
        });
    }

    private void playNext() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.playNext();
            } catch (Throwable e) {
                Log.w(TAG, String.format("Review logic: %s", e.getMessage()));
            }
        }

    }

    private void playPrevious() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.playPrevious();
            } catch (Throwable e) {
                Log.w(TAG, String.format("Review logic: %s", e.getMessage()));
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Constants.ACTION_MEDIA_PLAYER_STOPPED);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_PLAY);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_PAUSED);
        registerReceiver(broadcastReceiver, filter);

        refreshFD();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(broadcastReceiver);
    }

    private void refreshFD() {
        if (mediaPlayer != null) {
            mediaFD = mediaPlayer.getCurrentFD();
        }

        if (mediaFD != null) {
            refreshUIData();
        }
    }

    // media playerControl controls

    private static final int SHOW_PROGRESS = 1;

    private MediaPlayerControl playerControl;
    private ImageButton buttonPrevious;
    private ImageButton buttonPause;
    private ImageButton buttonNext;
    private ProgressBar progress;
    private TextView endTime;
    private TextView currentTime;

    private boolean dragging;

    private StringBuilder formatBuilder;
    private Formatter formatter;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
            case SHOW_PROGRESS:
                pos = setProgress();
                if (!dragging && playerControl != null && playerControl.isPlaying()) {
                    msg = obtainMessage(SHOW_PROGRESS);
                    sendMessageDelayed(msg, 1000 - (pos % 1000));
                }
                break;
            }
        }
    };

    private View.OnClickListener pauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            sync();
        }
    };

    private View.OnLongClickListener stopListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            stop();
            UXStats.instance().log(UXAction.PLAYER_STOP_ON_LONG_CLICK);
            return true;
        }
    };

    private View.OnClickListener previousListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (playerControl != null) {
                if (playerControl.getCurrentPosition() < 5000) {
                    mediaPlayer.playPrevious();
                } else {
                    playerControl.seekTo(0);
                }
            }
        }
    };

    private View.OnClickListener nextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (playerControl != null) {
                mediaPlayer.playNext();
            }
        }
    };

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private OnSeekBarChangeListener seekListener = new OnSeekBarChangeListener() {
        private long lastProgressChanged = 0;
        
        public void onStartTrackingTouch(SeekBar bar) {
            sync();

            dragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            handler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programatically generated changes to
                // the progress bar's position.
                return;
            }
            
            long now = System.currentTimeMillis();
            if (now - lastProgressChanged > 1000) {
                if (playerControl != null) {
                    long duration = playerControl.getDuration();
                    long newposition = (duration * progress) / 1000L;
                    playerControl.seekTo((int) newposition);
                    if (currentTime != null) {
                        currentTime.setText(stringForTime((int) newposition));
                    }
                }
                lastProgressChanged = now;
            }
        }

        public void onStopTrackingTouch(SeekBar bar) {
            dragging = false;
            setProgress();
            updatePausePlay();
            sync();

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            handler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    public void setMediaPlayerControl(MediaPlayerControl player) {
        this.playerControl = player;
        updatePausePlay();
    }

    public void sync() {
        setProgress();
        if (buttonPause != null) {
            buttonPause.requestFocus();
        }
        disableUnsupportedButtons();
        updatePausePlay();

        // cause the progress bar to be updated This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        handler.sendEmptyMessage(SHOW_PROGRESS);

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                sync();
                if (buttonPause != null) {
                    buttonPause.requestFocus();
                }
            }
            return true;

        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            if (uniqueDown && playerControl.isPlaying()) {
                updatePausePlay();
                sync();
                playerControl.stop();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        }

        sync();
        return super.dispatchKeyEvent(event);
    }

    /*
    @Override
    public void setEnabled(boolean enabled) {
        if (buttonPause != null) {
            buttonPause.setEnabled(enabled);
        }
        if (progress != null) {
            progress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }
    */

    private void disableUnsupportedButtons() {
        try {
            if (buttonPause != null && !playerControl.canPause()) {
                buttonPause.setEnabled(false);
            }
        } catch (Throwable e) {
        }
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        formatBuilder.setLength(0);
        formatBuilder.trimToSize();

        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (playerControl == null || dragging) {
            return 0;
        }
        int position = playerControl.getCurrentPosition();
        int duration = playerControl.getDuration();
        if (progress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                progress.setProgress((int) pos);
            }
            int percent = playerControl.getBufferPercentage();
            progress.setSecondaryProgress(percent * 10);
        }

        if (endTime != null) {
            endTime.setText(stringForTime(duration));
        }
        if (currentTime != null) {
            currentTime.setText(stringForTime(position));
        }

        return position;
    }

    private void updatePausePlay() {
        if (buttonPause == null || playerControl == null) {
            return;
        }

        if (playerControl.isPlaying()) {
            buttonPause.setImageResource(R.drawable.player_pause_icon);
        } else {
            buttonPause.setImageResource(R.drawable.player_play_icon);
        }
        buttonPause.setBackgroundDrawable(null);
    }

    private void doPauseResume() {
        if (playerControl == null) {
            return;
        }

        if (playerControl.isPlaying()) {
            playerControl.pause();
        } else {
            playerControl.resume();
        }

        updatePausePlay();
    }

    private void showPlayerContextMenu() {
        if (mediaFD == null) {
            return;
        }

        ContextMenuItem share = new ContextMenuItem(mediaFD.shared ? R.string.unshare : R.string.share, mediaFD.shared ? R.drawable.contextmenu_icon_unshare : R.drawable.contextmenu_icon_share) {
            @Override
            public void onClick() {
                mediaFD.shared = !mediaFD.shared;
                Librarian.instance().updateSharedStates(mediaFD.fileType, Arrays.asList(mediaFD));
                UXStats.instance().log(mediaFD.shared ? UXAction.PLAYER_MENU_SHARE : UXAction.PLAYER_MENU_UNSHARE);
            }
        };

        ContextMenuItem stop = new ContextMenuItem(R.string.stop, R.drawable.contextmenu_icon_stop) {
            @Override
            public void onClick() {
                stop();
                UXStats.instance().log(UXAction.PLAYER_MENU_STOP);
            }
        };

        ContextMenuItem delete = new ContextMenuItem(R.string.delete_this_track, R.drawable.contextmenu_icon_trash) {
            @Override
            public void onClick() {
                UIUtils.showYesNoDialog(MediaPlayerActivity.this, R.string.are_you_sure_delete_current_track, R.string.application_label, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onDeleteCurrentTrack();
                        UXStats.instance().log(UXAction.PLAYER_MENU_DELETE_TRACK);
                    }
                });
            }

        };

        ContextMenuDialog menu = new ContextMenuDialog();
        menu.setItems(Arrays.asList(share, stop, delete));
        menu.show(getFragmentManager(), "playerContextMenu");
    }

    private void onDeleteCurrentTrack() {
        final FileDescriptor currentFD = mediaPlayer.getCurrentFD();
        PlaylistItem currentPlaylistItem = new PlaylistItem(currentFD);
        Playlist playlist = mediaPlayer.getPlaylist();

        AsyncTask<Void, Void, Void> asyncDeleteTrackTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Librarian.instance().deleteFiles(Constants.FILE_TYPE_AUDIO, new ArrayList<FileDescriptor>(Arrays.asList(currentFD)));
                return null;
            }
        };

        if (playlist != null) {
            PlaylistItem nextItem = playlist.getNextItem();
            if (nextItem == null || nextItem.equals(currentPlaylistItem)) {
                asyncDeleteTrackTask.execute();
                stop();
                return;
            }
        }

        mediaPlayer.playNext();
        asyncDeleteTrackTask.execute();
    }
}