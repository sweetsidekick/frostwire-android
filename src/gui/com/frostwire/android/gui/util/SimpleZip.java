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

package com.frostwire.android.gui.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.util.Log;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class SimpleZip {

    private static final String TAG = "FW.SimpleZip";

    private static final int BUFFER_SIZE = 2048;

    private SimpleZip() {
    }

    public static boolean uncompress(String filename, String location) {
        File file = new File(location);
        if (!file.exists()) {
            file.mkdirs();
        } else {
            file.delete();
        }

        try {
            FileInputStream fin = new FileInputStream(filename);
            ZipInputStream zin = new ZipInputStream(fin);

            Log.d(TAG, "About to extract: " + filename);

            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {

                Log.d(TAG, "Extracting: " + ze.getName());

                String innerFileName = location + File.separator + ze.getName();
                File innerFile = new File(innerFileName);
                if (innerFile.exists()) {
                    innerFile.delete();
                }

                if (ze.isDirectory()) {
                    innerFile.mkdirs();
                } else {
                    FileOutputStream outputStream = new FileOutputStream(innerFileName);

                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER_SIZE);

                    int count = 0;
                    byte[] data = new byte[BUFFER_SIZE];
                    while ((count = zin.read(data, 0, BUFFER_SIZE)) != -1) {
                        bufferedOutputStream.write(data, 0, count);
                    }

                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                }

                zin.closeEntry();
            }

            zin.close();

            Log.d(TAG, "Done uncompressing: " + filename);

            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error uncompressing: " + filename, e);
        }

        return false;
    }
}
