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

package com.frostwire.android.util;

import java.io.File;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.frostwire.android.util.DiskCache.Entry;
import com.squareup.picasso.Cache;
import com.squareup.picasso.LruCache;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
final class ImageCache implements Cache {

    private final DiskCache disk;
    private final LruCache mem;

    public ImageCache(File directory, long diskSize, int memSize) {
        this.disk = createDiskCache(directory, diskSize);
        this.mem = new LruCache(memSize);
    }

    @Override
    public Bitmap get(String key) {
        Bitmap bmp = mem.get(key);

        if (bmp == null) {
            bmp = diskGet(key);
        }

        return bmp;
    }

    @Override
    public void set(String key, Bitmap bitmap) {
        mem.set(key, bitmap);

        diskPut(key, bitmap);
    }

    @Override
    public int size() {
        return mem.size() + diskSize();
    }

    @Override
    public int maxSize() {
        return mem.maxSize() + diskMaxSize();
    }

    @Override
    public void clear() {
        mem.clear();
    }

    private byte[] getBytes(Bitmap bmp) {
        ByteBuffer buffer = ByteBuffer.allocate(bmp.getByteCount());
        bmp.copyPixelsToBuffer(buffer);
        return buffer.array();
    }

    private DiskCache createDiskCache(File directory, long diskSize) {
        try {
            return new DiskCache(directory, diskSize);
        } catch (Throwable e) {
            return null;
        }
    }

    private Bitmap diskGet(String key) {
        Bitmap bmp = null;

        if (disk != null) {
            try {
                Entry e = disk.get(key);
                if (e != null) {
                    try {
                        bmp = BitmapFactory.decodeStream(e.getInputStream());
                    } finally {
                        e.close();
                    }

                    if (bmp == null) { // some error decoding
                        disk.remove(key);
                    }
                }
            } catch (Throwable e) {
                // ignore
            }
        }

        return bmp;
    }

    private void diskPut(String key, Bitmap bitmap) {
        if (disk != null) {
            try {
                disk.put(key, getBytes(bitmap));
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    private int diskSize() {
        return disk != null ? (int) disk.size() : 0;
    }

    private int diskMaxSize() {
        return disk != null ? (int) disk.maxSize() : 0;
    }
}
