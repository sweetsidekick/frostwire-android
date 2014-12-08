/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Modified by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
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

package com.frostwire.android;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.SystemClock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CompatMediaPlayer extends MediaPlayer implements OnCompletionListener {
    
    private boolean mCompatMode = true;
    private MediaPlayer mNextPlayer;
    private OnCompletionListener mCompletion;

    private Method m;

    public CompatMediaPlayer() {
        try {
            m = MediaPlayer.class.getDeclaredMethod("setNextMediaPlayer", MediaPlayer.class);
            mCompatMode = false;
        } catch (NoSuchMethodException e) {
            mCompatMode = true;
            super.setOnCompletionListener(this);
        }
    }

    public void setNextMediaPlayerSupport(MediaPlayer next) {
        if (mCompatMode) {
            mNextPlayer = next;
        } else {
            //super.setNextMediaPlayer(next);
            try {
                m.invoke(this, next);
            } catch (InvocationTargetException e) {
                Throwable ex = e.getTargetException();

                if (ex instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) ex;
                } else if (ex instanceof IllegalStateException) {
                    throw (IllegalStateException) ex;
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        if (mCompatMode) {
            mCompletion = listener;
        } else {
            super.setOnCompletionListener(listener);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        try {
            if (mNextPlayer != null) {
                // as it turns out, starting a new MediaPlayer on the completion
                // of a previous player ends up slightly overlapping the two
                // playbacks, so slightly delaying the start of the next player
                // gives a better user experience
                SystemClock.sleep(50);
                mNextPlayer.start();

            }
            mCompletion.onCompletion(this);
        } catch (Throwable e) {
            // yes, some times we get NPE due to sync issues
        }
    }
}
