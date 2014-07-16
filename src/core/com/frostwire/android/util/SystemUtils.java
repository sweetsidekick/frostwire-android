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

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;

import com.frostwire.logging.Logger;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class SystemUtils {

    private static final Logger LOG = Logger.getLogger(SystemUtils.class);

    private static final int KITKAT = 19;

    private SystemUtils() {
    }

    public static File getCacheDir(Context context, String directory) {
        File cache = null;

        if (isPrimaryExternalStorageMounted()) {
            cache = context.getExternalCacheDir();
        } else {
            cache = context.getCacheDir();
        }

        return new File(cache, directory);
    }

    public static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap) {
            memoryClass = am.getLargeMemoryClass();
        }
        // Target ~15% of the available heap.
        return 1024 * 1024 * memoryClass / 7;
    }

    public static long calculateDiskCacheSize(File dir, int minSize, int maxSize) {
        long size = minSize;

        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            // Target 2% of the total space.
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }

        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, maxSize), minSize);
    }

    public static boolean isPrimaryExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Use this instead of EnvironmentCompat.
     * 
     * @param path
     * @return
     */
    public static boolean isSecondaryExternalStorageMounted(File path) {
        if (path == null) { // fast precondition
            return false;
        }

        boolean result = false;

        if (Build.VERSION.SDK_INT >= KITKAT) {
            result = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(path));
        } else {
            try {
                String[] l = path.list();
                result = l != null && l.length > 0;
            } catch (Throwable e) {
                LOG.error("Error detecting secondary external storage state", e);
            }
        }

        return result;
    }

    public static long getAvailableStorageSize(File dir) {
        long size = -1;
        try {
            StatFs stat = new StatFs(dir.getAbsolutePath());
            size = ((long) stat.getAvailableBlocks()) * stat.getBlockSize();
        } catch (Throwable e) {
            size = -1; // system error computing the available storage size
        }

        return size;
    }

    /**
     * 
     * Use this instead ContextCompat
     * 
     * @param context
     * @return
     */
    public static File[] getExternalFilesDirs(Context context) {
        if (Build.VERSION.SDK_INT >= KITKAT) {
            List<File> dirs = new LinkedList<File>();

            for (File f : ContextCompat.getExternalFilesDirs(context, null)) {
                if (f != null) {
                    dirs.add(f);
                }
            }

            return dirs.toArray(new File[0]);
        } else {
            List<File> dirs = new LinkedList<File>();

            dirs.add(context.getExternalFilesDir(null));

            try {
                String secondaryStorages = System.getenv("SECONDARY_STORAGE");
                if (secondaryStorages != null) {
                    String[] storages = secondaryStorages.split(File.pathSeparator);
                    for (String s : storages) {
                        dirs.add(new File(s));
                    }
                }
            } catch (Throwable e) {
                LOG.error("Unable to get secondary external storages", e);
            }

            return dirs.toArray(new File[0]);
        }
    }
}
