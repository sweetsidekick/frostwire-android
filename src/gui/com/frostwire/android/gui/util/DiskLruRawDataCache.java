/*
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

package com.frostwire.android.gui.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.apache.commons.io.IOUtils;

import android.util.Log;

import com.frostwire.android.BuildConfig;
import com.frostwire.util.ByteUtils;
import com.jakewharton.disklrucache.DiskLruCache;

// Taken from
// http://stackoverflow.com/questions/10185898/using-disklrucache-in-android-4-0-does-not-provide-for-opencache-method
public class DiskLruRawDataCache {

    private DiskLruCache mDiskCache;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    //private static final String TAG = "DiskLruImageCache";

    private static final int IO_BUFFER_SIZE = 4 * 1024;

    public DiskLruRawDataCache(File diskCacheDir, int diskCacheSize) {
        try {
            mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean writeBytesToFile(byte[] data, DiskLruCache.Editor editor) throws IOException, FileNotFoundException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
            return IOUtils.copy(new ByteArrayInputStream(data), out) > 0;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private String encodeKey(Object key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5hash = new byte[32];
            byte[] bytes = key.toString().getBytes("utf-8");
            md.update(bytes, 0, bytes.length);
            md5hash = md.digest();
            return ByteUtils.encodeHex(md5hash);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return key.toString();
    }

    public void put(Object key, byte[] data) {
        put(encodeKey(key), data);
    }

    private void put(String key, byte[] data) {

        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskCache.edit(key);
            if (editor == null) {
                return;
            }

            if (writeBytesToFile(data, editor)) {
                mDiskCache.flush();
                editor.commit();
                if (BuildConfig.DEBUG) {
                    Log.d("cache_test_DISK_", "image put on disk cache " + key);
                }
            } else {
                editor.abort();
                if (BuildConfig.DEBUG) {
                    Log.d("cache_test_DISK_", "ERROR on: image put on disk cache " + key);
                }
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.d("cache_test_DISK_", "ERROR on: image put on disk cache " + key);
            }
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public byte[] getBytes(Object key) {
        return getBytes(encodeKey(key));
    }

    private byte[] getBytes(String key) {

        byte[] data = null;
        DiskLruCache.Snapshot snapshot = null;
        try {

            snapshot = mDiskCache.get(key);
            if (snapshot == null) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn = new BufferedInputStream(in, IO_BUFFER_SIZE);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                try {
                    IOUtils.copy(buffIn, baos);
                    data = baos.toByteArray();
                } finally {
                    baos.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d("cache_test_DISK_", data == null ? "" : "data read from disk " + key);
        }

        return data;

    }

    public boolean containsKey(Object key) {
        return containsKey(encodeKey(key));
    }

    private boolean containsKey(String key) {

        boolean contained = false;

        if (mDiskCache != null) {
            DiskLruCache.Snapshot snapshot = null;
            try {
                snapshot = mDiskCache.get(key);
                contained = snapshot != null;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (snapshot != null) {
                    snapshot.close();
                }
            }
        }

        return contained;

    }

    public void clearCache() {
        if (mDiskCache != null) {
            if (BuildConfig.DEBUG) {
                Log.d("cache_test_DISK_", "disk cache CLEARED");
            }
            try {
                mDiskCache.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
