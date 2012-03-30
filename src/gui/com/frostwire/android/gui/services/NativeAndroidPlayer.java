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

package com.frostwire.android.gui.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.core.player.CoreMediaPlayer;
import com.frostwire.android.core.player.Playlist;
import com.frostwire.android.core.player.PlaylistItem;
import com.frostwire.android.gui.activities.MediaPlayerActivity;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class NativeAndroidPlayer implements CoreMediaPlayer, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = "FW.NativeAndroidPlayer";

    private final Service service;
    private final FocusListener focusListener;

    private MediaPlayer mp;
    private Playlist playlist;
    private FileDescriptor currentFD;
    private boolean launchActivity;

    public NativeAndroidPlayer(Service service) {
        this.service = service;
        this.focusListener = new FocusListener();
    }

    @Override
    public void play(Playlist playlist) {
        stop();

        try {
            if (setupMediaPlayer()) {
                this.playlist = playlist;

                launchActivity = true;
                playNext();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error in playMedia", e);
            releaseMediaPlayer();
        }
    }

    @Override
    public void playPrevious() {
        if (mp != null) {
            PlaylistItem item = playlist.getPreviousItem();
            if (item != null && item.getFD() != null) {
                playInternal(item.getFD());
            }
        }
    }

    @Override
    public void playNext() {
        if (mp != null) {
            PlaylistItem item = playlist.getNextItem();
            if (item != null && item.getFD() != null) {
                playInternal(item.getFD());
            }
        }
    }

    @Override
    public void togglePause() {
        try {
            if (mp != null) {
                if (mp.isPlaying()) {
                    mp.pause();
                } else {
                    mp.start();
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error performing media player pause", e);
        }
    }

    @Override
    public void stop() {
        if (mp != null) {
            service.stopForeground(true);
        }

        releaseMediaPlayer();
        ((NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Constants.NOTIFICATION_MEDIA_PLAYING_ID);
        service.sendBroadcast(new Intent(Constants.ACTION_MEDIA_PLAYER_STOPPED));
    }

    @Override
    public boolean isPlaying() {
        return (mp!=null) ? mp.isPlaying() :  false;
    }
    
    @Override
    public void seekTo(int position) {
        if (mp != null) {
            mp.seekTo(position);
        }
    }
    
    @Override
    public int getPosition() {
        return (mp != null) ? mp.getCurrentPosition() : -1;
    }
    
    @Override
    public FileDescriptor getCurrentFD() {
        return currentFD;
    }

    public MediaPlayer getMediaPlayer() {
        return mp;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNext();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Toast.makeText(service, service.getString(R.string.media_player_failed), Toast.LENGTH_SHORT).show();

        //releaseMediaPlayer();
        //service.stopForeground(true);

        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp != null) {
            mp.start();

            if (launchActivity) {
                launchActivity = false;
                notifyMediaPlay();
            }
            service.sendBroadcast(new Intent(Constants.ACTION_MEDIA_PLAYER_PLAY));
        }
    }

    private boolean setupMediaPlayer() {
        mp = new MediaPlayer();
        mp.setOnPreparedListener(this);
        mp.setOnErrorListener(this);
        mp.setOnCompletionListener(this);
        mp.setWakeMode(service.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void releaseMediaPlayer() {
        if (mp != null) {
            mp.stop();
            mp.release();
        }

        mp = null;
        playlist = null;
        currentFD = null;
    }

    private void playInternal(FileDescriptor fd) {
        if (mp != null && fd != null) {
            try {
                currentFD = fd;

                mp.stop();
                mp.reset();
                mp.setDataSource(currentFD.filePath);
                mp.prepareAsync();
            } catch (Throwable e) {
                Log.e(TAG, "Error playing media: " + currentFD.filePath, e);
            }
        }
    }

    private void notifyMediaPlay() {
        try {
            Context context = service.getApplicationContext();

            Intent i = new Intent(context, MediaPlayerActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification();
            notification.tickerText = service.getString(R.string.playing_song_name, currentFD.title);
            notification.icon = R.drawable.play_notification;
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.setLatestEventInfo(context, service.getString(R.string.application_label), service.getString(R.string.playing_song_name, currentFD.title), pi);
            service.startForeground(Constants.NOTIFICATION_MEDIA_PLAYING_ID, notification);

            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            
        } catch (Throwable e) {
            Log.e(TAG, "Error creating player notification", e);
        }
    }

    private final class FocusListener implements OnAudioFocusChangeListener {

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                togglePause();
            }
        }
    }
}
