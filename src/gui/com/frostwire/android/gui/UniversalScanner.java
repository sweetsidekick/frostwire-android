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

package com.frostwire.android.gui;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.core.MediaType;
import com.frostwire.android.core.providers.UniversalStore;
import com.frostwire.android.core.providers.UniversalStore.Documents;
import com.frostwire.android.core.providers.UniversalStore.Documents.DocumentsColumns;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.logging.Logger;
import com.frostwire.util.Condition;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
final class UniversalScanner {

    private static final Logger LOG = Logger.getLogger(UniversalScanner.class);

    private final Context context;

    public UniversalScanner(Context context) {
        this.context = context;
    }

    public void scan(final String filePath) {
        scan(Arrays.asList(new File(filePath)));
    }
    
    public void scan(final Collection<File> filesToScan) {
        new MultiFileAndroidScanner(filesToScan).scan();
    }


    private static void shareFinishedDownload(FileDescriptor fd) {
        if (fd != null) {
            if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TRANSFER_SHARE_FINISHED_DOWNLOADS)) {
                fd.shared = true;
                Librarian.instance().updateSharedStates(fd.fileType, Arrays.asList(fd));
            }
            Librarian.instance().invalidateCountCache(fd.fileType);
        }
    }

    private void scanDocument(String filePath) {
        File file = new File(filePath);

        if (documentExists(filePath, file.length())) {
            return;
        }

        String displayName = FilenameUtils.getBaseName(file.getName());

        ContentResolver cr = context.getContentResolver();

        ContentValues values = new ContentValues();

        values.put(DocumentsColumns.DATA, filePath);
        values.put(DocumentsColumns.SIZE, file.length());
        values.put(DocumentsColumns.DISPLAY_NAME, displayName);
        values.put(DocumentsColumns.TITLE, displayName);
        values.put(DocumentsColumns.DATE_ADDED, System.currentTimeMillis());
        values.put(DocumentsColumns.DATE_MODIFIED, file.lastModified());
        values.put(DocumentsColumns.MIME_TYPE, UIUtils.getMimeType(filePath));

        Uri uri = cr.insert(Documents.Media.CONTENT_URI, values);

        FileDescriptor fd = new FileDescriptor();
        fd.fileType = Constants.FILE_TYPE_DOCUMENTS;
        fd.id = Integer.valueOf(uri.getLastPathSegment());

        shareFinishedDownload(fd);
    }

    private boolean documentExists(String filePath, long size) {
        boolean result = false;

        Cursor c = null;

        try {
            ContentResolver cr = context.getContentResolver();
            c = cr.query(UniversalStore.Documents.Media.CONTENT_URI, new String[] { DocumentsColumns._ID }, DocumentsColumns.DATA + "=?" + " AND " + DocumentsColumns.SIZE + "=?", new String[] { filePath, String.valueOf(size) }, null);
            result = c != null && c.getCount() != 0;
        } catch (Throwable e) {
            LOG.warn("Error detecting if file exists: " + filePath, e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return result;
    }

    private final class MultiFileAndroidScanner implements MediaScannerConnectionClient {

        private MediaScannerConnection connection;
        private final Collection<File> files;
        private int numCompletedScans;

        public MultiFileAndroidScanner(Collection<File> filesToScan) {
            this.files = filesToScan;
            numCompletedScans = 0;
        }

        public void scan() {
            try {
                connection = new MediaScannerConnection(context, this);
                connection.connect();
            } catch (Throwable e) {
                LOG.warn("Error scanning file with android internal scanner, one retry", e);
                SystemClock.sleep(1000);
                connection = new MediaScannerConnection(context, this);
                connection.connect();
            }
        }

        public void onMediaScannerConnected() {
            try {
                /** should only arrive here on connected state, but let's double check since it's possible */
                if (connection.isConnected() && files != null && !files.isEmpty()) {
                    for (File f : files) {
                        connection.scanFile(f.getAbsolutePath(), null);
                    }
                }
            } catch (IllegalStateException e) {
                LOG.warn("Scanner service wasn't really connected or service was null", e);
                //should we try to connect again? don't want to end up in endless loop
                //maybe destroy connection?
            }
        }

        
		public void onScanCompleted(String path, Uri uri) {
            /** This will work if onScanCompleted is invoked after scanFile finishes. */
            numCompletedScans++;
            if (numCompletedScans == files.size()) {
                connection.disconnect();
            }
            
            MediaType mt = MediaType.getMediaTypeForExtension(FilenameUtils.getExtension(path));
            
            if (uri != null && !path.contains("/Android/data/" + context.getPackageName())) {                
                if (mt != null && mt.getId() == Constants.FILE_TYPE_DOCUMENTS) {
                    scanDocument(path);
                } else {
                    //LOG.debug("Scanned new file: " + uri);
                    shareFinishedDownload(Librarian.instance().getFileDescriptor(uri));
                }
            } else {
                if (path.endsWith(".apk")) {
                    //LOG.debug("Can't scan apk for security concerns: " + path);
                } else if (mt != null) {
                	if (mt.getId() == Constants.FILE_TYPE_PICTURES) {
                		scanPrivatePicture(path);
                	}
                	if (mt.getId() == Constants.FILE_TYPE_AUDIO ||
                	    mt.getId() == Constants.FILE_TYPE_VIDEOS) {
                		scanPrivateFile(path, mt);
                	}
                }
                else {
                    scanDocument(path);
                    //LOG.debug("Scanned new file as document: " + path);
                }
            }
        }
    }

    /**
     * Android geniuses put a .nomedia file on the .../Android/data/ folder
     * inside the secondary external storage path, therefore, all attempts
     * to use MediaScannerConnection to scan a media file fail. Therefore we
     * have this method to insert the file's metadata manually on the content provider.
     * @param path
     */
	public void scanPrivateFile(String filePath, MediaType mt) {
		File file = new File(filePath);

        String displayName = FilenameUtils.getBaseName(file.getName());
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        
        Uri extContentUri = null;
        int mtId = mt.getId();
        
        switch (mtId) {
           case Constants.FILE_TYPE_AUDIO:
        	   extContentUri = Audio.Media.EXTERNAL_CONTENT_URI;
        	   break;
           case Constants.FILE_TYPE_VIDEOS:
        	   extContentUri = Video.Media.EXTERNAL_CONTENT_URI;
               break;
           
        }

		if (extContentUri != null) {
			//using Audio... keys, they're all the same.
			values.put(Audio.Media.DISPLAY_NAME, displayName);
			values.put(Audio.Media.SIZE, file.length());
			values.put(Audio.AudioColumns.DATA, file.getAbsolutePath());

			Uri uri = cr.insert(extContentUri, values);

			FileDescriptor fd = new FileDescriptor();
			fd.fileType = (byte) mtId;
			fd.id = Integer.valueOf(uri.getLastPathSegment());

			shareFinishedDownload(fd);
		}
	}

	public void scanPrivatePicture(String path) {
	    try {
	    	ContentResolver cr = context.getContentResolver();
	    	File f = new File(path);
	    	Images.Media.insertImage(cr, f.getAbsolutePath(), f.getName(), f.getName());
	    } catch (Throwable t) {
	    	t.printStackTrace();
	    }
		
	}
}