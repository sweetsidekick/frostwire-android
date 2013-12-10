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

import java.util.ArrayList;
import java.util.List;

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
    private final List<FileDescriptor> failedFDs;

    private MediaPlayer mp;
    private Playlist playlist;
    private FileDescriptor currentFD;
    private boolean launchActivity;

    public NativeAndroidPlayer(Service service) {
        this.service = service;
        this.focusListener = new FocusListener();
        this.failedFDs = new ArrayList<FileDescriptor>();
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
        if (mp != null && playlist != null) {
            PlaylistItem item = playlist.getPreviousItem();
            if (item != null && item.getFD() != null) {
                playInternal(item.getFD());
            }
        }
    }

    @Override
    public void playNext() {
        if (mp != null && playlist != null) {
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
                    notifyMediaPaused(false);
                } else {
                    mp.start();
                    notifyMediaPlay(false);
                }
                
                service.sendBroadcast(new Intent(mp.isPlaying() ? Constants.ACTION_MEDIA_PLAYER_PLAY : Constants.ACTION_MEDIA_PLAYER_PAUSED));
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error performing media player pause", e);
        }
    }

    public void stop() {
        if (mp != null) {
            service.stopForeground(true);
        }

        releaseMediaPlayer();
        int notificationId = isPlaying() ? Constants.NOTIFICATION_MEDIA_PLAYING_ID : Constants.NOTIFICATION_MEDIA_PAUSED_ID;
        ((NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
        service.sendBroadcast(new Intent(Constants.ACTION_MEDIA_PLAYER_STOPPED));

    }
    
    @Override
    public boolean isPlaying() {
        return (mp != null) ? mp.isPlaying() : false;
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
        if (currentFD != null) {
            failedFDs.add(currentFD);
            Log.e(TAG, String.format("Error playing media, what=%d, file=%s", what, currentFD.filePath));
        }

        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp != null) {
            try {
                mp.start();

                notifyMediaPlay(launchActivity);

                if (launchActivity) {
                    launchActivity = false;
                }

                service.sendBroadcast(new Intent(Constants.ACTION_MEDIA_PLAYER_PLAY));
            } catch (Throwable e) {
                if (currentFD != null) {
                    failedFDs.add(currentFD);
                }
                Log.e(TAG, String.format("Error starting media player for file: %s", currentFD != null ? currentFD.filePath : "unknown"), e);
            }
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

        failedFDs.clear();
    }

    private void playInternal(FileDescriptor fd) {
        if (mp != null && fd != null) {
            try {
                if (failedFDs.contains(fd)) {
                    Log.w(TAG, String.format("File play just failed: %s", fd.filePath));
                    return;
                }

                currentFD = fd;

                mp.stop();
                mp.reset();
                mp.setDataSource(currentFD.filePath);
                mp.prepareAsync();
            } catch (Throwable e) {
                failedFDs.add(currentFD);
                Log.e(TAG, String.format("Error playing media: %s", currentFD.filePath), e);
            }
        }
    }

    private void notifyMediaPlay(boolean launchActivity) {
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

            ((NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
            service.startForeground(Constants.NOTIFICATION_MEDIA_PLAYING_ID, notification);

            if (launchActivity) {
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error creating player notification", e);
        }
    }
    
    private void notifyMediaPaused(boolean launchActivity) {
        try {
            Context context = service.getApplicationContext();

            Intent i = new Intent(context, MediaPlayerActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification();
            notification.tickerText = service.getString(R.string.paused_song_name, currentFD.title);
            notification.icon = R.drawable.pause_notification;
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.setLatestEventInfo(context, service.getString(R.string.application_label), service.getString(R.string.paused_song_name, currentFD.title), pi);

            ((NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
            service.startForeground(Constants.NOTIFICATION_MEDIA_PAUSED_ID, notification);

            if (launchActivity) {
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error creating player notification", e);
        }        
    }

    private final class FocusListener implements OnAudioFocusChangeListener {
        
        private boolean shouldUnpause;
        
        @Override
        public void onAudioFocusChange(int focusChange) {

            AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
            int audioMode = am.getMode();
            
            shouldUnpause = (audioMode == AudioManager.MODE_IN_CALL ||
                    //audioMode == AudioManager.MODE_IN_COMMUNICATION ||  // not in api 10
                    audioMode == AudioManager.MODE_RINGTONE) &&
                    isPlaying();
            
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                if (isPlaying() || shouldUnpause) {
                    shouldUnpause = false;
                    togglePause();
                }
            }
        }
    }

    @Override
    public Playlist getPlaylist() {
        return playlist;
    }

    public void start() {
        if (mp != null) {
            mp.start();
            notifyMediaPlay(false);
            service.sendBroadcast(new Intent(Constants.ACTION_MEDIA_PLAYER_PLAY));
        }
    }

    public int getDuration() {
        int duration = 0;
        if (mp != null) {
            duration = mp.getDuration();
        }
        return duration;
    }

    public int getCurrentPosition() {
        int currentPos = 0;
        if (mp != null) {
            currentPos = mp.getCurrentPosition();
        }
        return currentPos;   
   }
}