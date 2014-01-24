/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 
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

package com.frostwire.localpeer;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class AndroidMulticastLock implements MulticastLock {

    private static final String LOCK_NAME_PREFIX = "FWLock_";
    private static final AtomicInteger lockCount = new AtomicInteger(0);

    private final android.net.wifi.WifiManager.MulticastLock lock;

    public AndroidMulticastLock(Context ctx) {
        WifiManager wifi = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);

        lock = wifi.createMulticastLock(LOCK_NAME_PREFIX + lockCount.getAndIncrement());
        lock.setReferenceCounted(true);
    }

    @Override
    public void acquire() {
        lock.acquire();
    }

    @Override
    public void release() {
        lock.release();
    }

    @Override
    public boolean isHeld() {
        return lock.isHeld();
    }

}
