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

package com.frostwire.android.util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class StorageUtils {

    private static final String TAG = "FW.StorageUtils";

    private static Method isExternalStorageRemovableM = null;
    private static Method isExternalStorageEmulatedM = null;

    static {
        try {
            isExternalStorageRemovableM = Environment.class.getDeclaredMethod("isExternalStorageRemovable");
        } catch (Throwable e) {
            // ignore
        }
        try {
            isExternalStorageEmulatedM = Environment.class.getDeclaredMethod("isExternalStorageEmulated");
        } catch (Throwable e) {
            // ignore
        }
    }

    private StorageUtils() {
    }

    public static List<StorageMount> getStorageMounts() {
        List<StorageMount> mounts = new ArrayList<StorageMount>();
        List<String> paths = getStoragePaths();

        int j = 0;
        for (int i = 0; i < paths.size(); i++) {
            // if we have a mount point, we have always /mnt/sdcard as the first one
            if (i == 0) {
                String label = null;
                if (Build.VERSION.SDK_INT < 9) { // Build.VERSION_CODES.GINGERBREAD
                    label = "Auto";
                } else if (Build.VERSION.SDK_INT < 11) { // Build.VERSION_CODES.HONEYCOMB
                    if (isExternalStorageRemovable()) {
                        label = "External SD Card 1";
                        j = 1;
                    } else {
                        label = "Internal Storage";
                    }
                } else {
                    if (!isExternalStorageRemovable() || isExternalStorageEmulated())
                        label = "Internal Storage";
                    else {
                        label = "External SD Card 1";
                        j = 1;
                    }
                }
                mounts.add(new StorageMount(label, paths.get(0)));
            } else {
                mounts.add(new StorageMount("External SD Card " + (i + j), paths.get(i)));
            }
        }

        return mounts;
    }

    /**
     * Read /proc/mounts
     */
    private static List<String> readMounts() {
        List<String> mounts = new ArrayList<String>();

        // ensure that the default path is the first in our list
        mounts.add("/mnt/sdcard");

        try {
            Scanner scanner = new Scanner(new File("/proc/mounts"));
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("/dev/block/vold/")) {
                    String[] lineElements = line.split(" ");
                    String element = lineElements[1];

                    // don't add the default mount path
                    if (!element.equals("/mnt/sdcard")) {
                        mounts.add(element);
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error reading /proc/mounts", e);
        }

        return mounts;
    }

    /**
     * Read /system/etc/vold.fstab
     * @return
     */
    private static List<String> readVold() {
        List<String> vold = new ArrayList<String>();

        // ensure that the default path exists and is the first in our list
        vold.add("/mnt/sdcard");

        try {
            Scanner scanner = new Scanner(new File("/system/etc/vold.fstab"));
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("dev_mount")) {
                    String[] lineElements = line.split(" ");
                    String element = lineElements[2];

                    if (element.contains(":"))
                        element = element.substring(0, element.indexOf(":"));

                    // don't add the default vold path
                    if (!element.equals("/mnt/sdcard")) {
                        vold.add(element);
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error reading /system/etc/vold.fstab", e);
        }

        return vold;
    }

    private static List<String> getStoragePaths() {
        List<String> mounts = readMounts();
        List<String> vold = readVold();

        // intersect both lists in mounts
        for (int i = 0; i < mounts.size(); i++) {
            String mount = mounts.get(i);
            if (!vold.contains(mount)) {
                mounts.remove(i);
                i--;
            }
        }

        // test if the path is valid and available
        for (int i = 0; i < mounts.size(); i++) {
            String mount = mounts.get(i);
            File root = new File(mount);
            if (!root.exists() || !root.isDirectory() || !root.canWrite() || root.isHidden()) {
                mounts.remove(i);
                i--;
            }
        }

        return mounts;
    }

    private static boolean isExternalStorageRemovable() {
        try {
            if (isExternalStorageRemovableM != null) {
                return (Boolean) isExternalStorageRemovableM.invoke(null);
            }
        } catch (Throwable e) {
            // ignore
        }

        return false;
    }

    private static boolean isExternalStorageEmulated() {
        try {
            if (isExternalStorageEmulatedM != null) {
                return (Boolean) isExternalStorageEmulatedM.invoke(null);
            }
        } catch (Throwable e) {
            // ignore
        }

        return false;
    }
}
