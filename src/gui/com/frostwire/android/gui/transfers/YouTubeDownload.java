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

package com.frostwire.android.gui.transfers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import android.util.Log;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.PropertyBoxParserImpl;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.coremedia.iso.boxes.FileTypeBox;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MetaBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.UserDataBox;
import com.coremedia.iso.boxes.apple.AppleAlbumArtistBox;
import com.coremedia.iso.boxes.apple.AppleAlbumBox;
import com.coremedia.iso.boxes.apple.AppleArtistBox;
import com.coremedia.iso.boxes.apple.AppleCoverBox;
import com.coremedia.iso.boxes.apple.AppleItemListBox;
import com.coremedia.iso.boxes.apple.AppleMediaTypeBox;
import com.coremedia.iso.boxes.apple.AppleTrackTitleBox;
import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.search.youtube.YouTubeCrawledSearchResult;
import com.frostwire.search.youtube.YouTubeDownloadLink;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Mp4TrackImpl;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class YouTubeDownload extends TemporaryDownloadTransfer<YouTubeCrawledSearchResult> {

    private static final String TAG = "FW.YouTubeDownload";

    private static final int STATUS_NONE = 0;
    private static final int STATUS_VERIFYING = 1;

    private int status;

    private final TransferManager manager;
    
    public YouTubeDownload(TransferManager manager, YouTubeCrawledSearchResult sr) {
        this.manager = manager;
        this.sr = sr;
    }

    @Override
    public String getDisplayName() {
        return sr.getDisplayName();
    }

    @Override
    public String getStatus() {
        if (status == STATUS_VERIFYING) {
            return String.valueOf(R.string.youtube_download_status_verifying);
        } else {
            return delegate != null ? delegate.getStatus() : "";
        }
    }

    @Override
    public int getProgress() {
        return delegate != null ? delegate.getProgress() : 0;
    }

    @Override
    public long getSize() {
        return delegate != null ? delegate.getSize() : 0;
    }

    @Override
    public Date getDateCreated() {
        return delegate != null ? delegate.getDateCreated() : new Date();
    }

    @Override
    public long getBytesReceived() {
        return delegate != null ? delegate.getBytesReceived() : 0;
    }

    @Override
    public long getBytesSent() {
        return delegate != null ? delegate.getBytesSent() : 0;
    }

    @Override
    public long getDownloadSpeed() {
        return delegate != null ? delegate.getDownloadSpeed() : 0;
    }

    @Override
    public long getUploadSpeed() {
        return delegate != null ? delegate.getUploadSpeed() : 0;
    }

    @Override
    public long getETA() {
        return delegate != null ? delegate.getETA() : 0;
    }

    @Override
    public boolean isComplete() {
        return delegate != null ? delegate.isComplete() : false;
    }

    @Override
    public List<? extends TransferItem> getItems() {
        return Collections.emptyList();
    }

    @Override
    public void cancel() {
        if (delegate != null) {
            delegate.cancel();
        }
        manager.remove(this);
    }

    @Override
    public boolean isDownloading() {
        return delegate != null ? delegate.isDownloading() : false;
    }

    @Override
    public void cancel(boolean deleteData) {
        if (delegate != null) {
            delegate.cancel(deleteData);
        }
        manager.remove(this);
    }

    public void start() {
        
        try {
            YouTubeDownloadLink ytLink = sr.getYouTubeDownloadLink();
            HttpDownloadLink link = new HttpDownloadLink(ytLink.getDownloadUrl(),ytLink.getFilename(),sr.getDisplayName(),ytLink.getSize(),false);
            if (link != null) {
                final boolean isAudio =sr.getYouTubeDownloadLink().isAudio();
                if (isAudio) {
                    link = link.withFilename(link.getFileName().replace(".mp4", ".m4a"));
                }
                
                final HttpDownloadLink finalLink = link;

                delegate = new HttpDownload(manager, SystemUtils.getTempDirectory(), link);
                delegate.setListener(new HttpDownloadListener() {
                    @Override
                    public void onComplete(HttpDownload download) {
                        if (isAudio) {
                            if (!demuxMP4Audio(finalLink, download, sr.getDetailsUrl())) {
                                // handle demux error here. Why? java.lang.RuntimeException: too many PopLocalFrame calls
                            }
                        }

                        moveFile(download.getSavePath(), !isAudio);

                        scanFinalFile();
                    }
                });
                delegate.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting youtube download", e);
        }
    }

    protected void moveFile(File savePath, boolean video) {
        super.moveFile(savePath, video ? Constants.FILE_TYPE_VIDEOS : Constants.FILE_TYPE_AUDIO);
    }

    private boolean demuxMP4Audio(HttpDownloadLink dl, HttpDownload delegate, String videoLink) {
        String filename = delegate.getSavePath().getAbsolutePath();
        try {
            status = STATUS_VERIFYING;
            String mp4Filename = filename.replace(".m4a", ".mp4");
            final String jpgFilename = filename.replace(".m4a", ".jpg");
            downloadThumbnail(dl, jpgFilename, videoLink);
            new File(filename).renameTo(new File(mp4Filename));
            FileInputStream fis = new FileInputStream(mp4Filename);
            fis.getFD().sync();
            FileChannel inFC = fis.getChannel();
            Movie inVideo = buildMovie(inFC);

            Track audioTrack = null;

            for (Track trk : inVideo.getTracks()) {
                if (trk.getHandler().equals("soun")) {
                    audioTrack = trk;
                    break;
                }
            }

            if (audioTrack == null) {
                Log.e(TAG, "No Audio track in MP4 file!!! - " + filename);
                fis.close();
                return false;
            }

            Movie outMovie = new Movie();
            outMovie.addTrack(audioTrack);

            IsoFile out = new DefaultMp4Builder() {
                protected FileTypeBox createFileTypeBox(Movie movie) {
                    List<String> minorBrands = new LinkedList<String>();
                    minorBrands.add("M4A ");
                    minorBrands.add("mp42");
                    minorBrands.add("isom");
                    minorBrands.add("\0\0\0\0");

                    return new FileTypeBox("M4A ", 0, minorBrands);
                };

                protected MovieBox createMovieBox(Movie movie, Map<Track, int[]> chunks) {
                    MovieBox moov = super.createMovieBox(movie, chunks);
                    moov.getMovieHeaderBox().setVersion(0);
                    return moov;
                };

                protected TrackBox createTrackBox(Track track, Movie movie, Map<Track, int[]> chunks) {
                    TrackBox trak = super.createTrackBox(track, movie, chunks);

                    TrackHeaderBox tkhd = trak.getTrackHeaderBox();
                    tkhd.setVersion(0);
                    tkhd.setVolume(1.0f);

                    return trak;
                };

                protected Box createUdta(Movie movie) {
                    return addUserDataBox(sr.getDisplayName(), sr.getSource(), jpgFilename);
                };
            }.build(outMovie);
            String audioFilename = filename;
            FileOutputStream fos = new FileOutputStream(audioFilename);
            out.getBox(fos.getChannel());
            fos.close();

            if (!new File(mp4Filename).delete()) {
                new File(mp4Filename).deleteOnExit();
            }
            File jpgFile = new File(jpgFilename);
            if (jpgFile.exists() && !jpgFile.delete()) {
                jpgFile.deleteOnExit();
            }

            IOUtils.closeQuietly(fis);

            return true;
        } catch (Throwable e) {
            Log.e(TAG, "Error demuxing MP4 audio - " + filename, e);
            return false;
        } finally {
            status = STATUS_NONE;
        }
    }
    
    public static Movie buildMovie(ReadableByteChannel channel) throws IOException {
        BoxParser parser = new PropertyBoxParserImpl() {
            @Override
            public Box parseBox(ReadableByteChannel byteChannel, ContainerBox parent) throws IOException {
                Box box = super.parseBox(byteChannel, parent);

                if (box instanceof AbstractBox) {
                    ((AbstractBox) box).parseDetails();
                }

                return box;
            }
        };
        IsoFile isoFile = new IsoFile(channel, parser);
        Movie m = new Movie();
        List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
        for (TrackBox trackBox : trackBoxes) {
            m.addTrack(new Mp4TrackImpl(trackBox));
        }
        
        
        IOUtils.closeQuietly(isoFile);
        
        return m;
    }
    
    private static UserDataBox addUserDataBox(String title, String author, String jpgFilename) {
        File jpgFile = new File(jpgFilename);
        if (!jpgFile.exists()) {
            return null;
        }

        byte[] jpgData = toByteArray(jpgFile);
        if (jpgData == null) {
            return null;
        }

        //"/moov/udta/meta/ilst/covr/data"
        UserDataBox udta = new UserDataBox();

        MetaBox meta = new MetaBox();
        udta.addBox(meta);

        HandlerBox hdlr = new HandlerBox();
        hdlr.setHandlerType("mdir");
        meta.addBox(hdlr);

        AppleItemListBox ilst = new AppleItemListBox();
        meta.addBox(ilst);

        AppleTrackTitleBox cnam = new AppleTrackTitleBox();
        cnam.setValue(title);
        ilst.addBox(cnam);

        AppleArtistBox cART = new AppleArtistBox();
        cART.setValue(author);
        ilst.addBox(cART);

        AppleAlbumArtistBox aART = new AppleAlbumArtistBox();
        aART.setValue(title + " " + author);
        ilst.addBox(aART);

        AppleAlbumBox calb = new AppleAlbumBox();
        calb.setValue(title + " " + author + " via YouTube.com");
        ilst.addBox(calb);

        AppleMediaTypeBox stik = new AppleMediaTypeBox();
        stik.setValue("1");
        ilst.addBox(stik);

        AppleCoverBox covr = new AppleCoverBox();
        covr.setJpg(jpgData);
        ilst.addBox(covr);

        return udta;
    }
    
    private static void downloadThumbnail(HttpDownloadLink dl, String jpgFilename, String videoLink) {
        try {
            //http://www.youtube.com/watch?v=[id]
            //http://i.ytimg.com/vi/[id]/hqdefault.jpg
            String id = videoLink.replace("http://www.youtube.com/watch?v=", "");
            String url = "http://i.ytimg.com/vi/" + id + "/hqdefault.jpg";
            HttpDownload.simpleHTTP(url, new FileOutputStream(jpgFilename), 3000);

        } catch (Throwable e) {
            Log.e(TAG, "Unable to get youtube thumbnail - " + dl.getFileName());
        }
    }
    
    private static byte[] toByteArray(File file) {
        InputStream in = null;

        try {
            int length = (int) file.length();
            byte[] array = new byte[length];
            in = new FileInputStream(file);

            int offset = 0;
            while (offset < length) {
                offset += in.read(array, offset, (length - offset));
            }
            in.close();
            return array;
        } catch (Throwable e) {
            Log.e(TAG, "Error reading local youtube thumbnail - " + file);
        }

        return null;
    }
}