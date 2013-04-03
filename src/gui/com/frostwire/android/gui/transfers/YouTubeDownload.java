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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import android.util.Log;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
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
import com.frostwire.android.util.FileUtils;
import com.frostwire.search.youtube.YouTubeSearchResult;
import com.frostwire.search.youtube.YouTubeSearchResult.ResultType;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class YouTubeDownload extends TemporaryDownloadTransfer<YouTubeSearchResult> {

    private static final String TAG = "FW.YouTubeDownload";

    static public final Pattern YT_FILENAME_PATTERN = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);

    private static final int STATUS_NONE = 0;
    private static final int STATUS_VERIFYING = 1;

    private int status;

    HashMap<DestinationFormat, ArrayList<Info>> possibleconverts = null;

    private final TransferManager manager;

    public YouTubeDownload(TransferManager manager, YouTubeSearchResult sr) {
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
            HttpDownloadLink link = decrypt();
            if (link != null) {

                if (sr.getResultType().equals(ResultType.AUDIO)) {
                    link = link.withFilename(link.getFileName().replace(".mp4", ".m4a"));
                }
                
                final HttpDownloadLink finalLink = link;

                delegate = new HttpDownload(manager, SystemUtils.getTempDirectory(), link);
                delegate.setListener(new HttpDownloadListener() {
                    @Override
                    public void onComplete(HttpDownload download) {
                        if (sr.getResultType().equals(ResultType.AUDIO)) {
                            if (!demuxMP4Audio(finalLink, download, sr.getDetailsUrl())) {
                                // handle demux error here. Why? java.lang.RuntimeException: too many PopLocalFrame calls
                            }
                        }

                        moveFile(download.getSavePath(), sr.getResultType().equals(ResultType.VIDEO));

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

    private HttpDownloadLink decrypt() throws Exception {
        this.possibleconverts = new HashMap<DestinationFormat, ArrayList<Info>>();
        HttpDownloadLink decryptedLink = null;

        String param = sr.getDetailsUrl();

        String parameter = param.toString().replace("watch#!v", "watch?v");
        parameter = parameter.replaceFirst("(verify_age\\?next_url=\\/?)", "");
        parameter = parameter.replaceFirst("(%3Fv%3D)", "?v=");
        parameter = parameter.replaceFirst("(watch\\?.*?v)", "watch?v");
        parameter = parameter.replaceFirst("/embed/", "/watch?v=");
        parameter = parameter.replaceFirst("https", "http");

        Browser br = new Browser();

        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies("youtube.com");

        if (parameter.contains("watch#")) {
            parameter = parameter.replace("watch#", "watch?");
        }
        if (parameter.contains("v/")) {
            String id = new Regex(parameter, "v/([a-z\\-_A-Z0-9]+)").getMatch(0);
            if (id != null)
                parameter = "http://www.youtube.com/watch?v=" + id;
        }

        boolean prem = false;

        try {
            final HashMap<Integer, String[]> LinksFound = this.getLinks(parameter, prem, br, 0);
            String error = br.getRegex("<div id=\"unavailable\\-message\" class=\"\">[\t\n\r ]+<span class=\"yt\\-alert\\-vertical\\-trick\"></span>[\t\n\r ]+<div class=\"yt\\-alert\\-message\">([^<>\"]*?)</div>").getMatch(0);
            if (error == null)
                error = br.getRegex("<div class=\"yt\\-alert\\-message\">(.*?)</div>").getMatch(0);
            if ((LinksFound == null || LinksFound.isEmpty()) && error != null) {
                //logger.info("Video unavailable: " + parameter);
                //logger.info("Reason: " + error.trim());
                return decryptedLink;
            }
            if (LinksFound == null || LinksFound.isEmpty()) {
                if (br.getURL().toLowerCase(Locale.getDefault()).indexOf("youtube.com/get_video_info?") != -1 && !prem) {
                    throw new IOException("DecrypterException.ACCOUNT");
                }
                throw new IOException("Video no longer available");
            }

            /* First get the filename */
            String YT_FILENAME = "";
            if (LinksFound.containsKey(-1)) {
                YT_FILENAME = LinksFound.get(-1)[0];
                LinksFound.remove(-1);
            }

            final boolean fast = false;//cfg.getBooleanProperty("FAST_CHECK2", false);
            //final boolean mp3 = cfg.getBooleanProperty("ALLOW_MP3", true);
            final boolean mp4 = true;//cfg.getBooleanProperty("ALLOW_MP4", true);
            //final boolean webm = cfg.getBooleanProperty("ALLOW_WEBM", true);
            //final boolean flv = cfg.getBooleanProperty("ALLOW_FLV", true);
            //final boolean threegp = cfg.getBooleanProperty("ALLOW_3GP", true);

            /* http://en.wikipedia.org/wiki/YouTube */
            final HashMap<Integer, Object[]> ytVideo = new HashMap<Integer, Object[]>() {
                /**
                 * 
                 */
                private static final long serialVersionUID = -3028718522449785181L;

                {
                    // **** FLV *****
                    //                if (mp3) {
                    //                    this.put(0, new Object[] { DestinationFormat.VIDEOFLV, "H.263", "MP3", "Mono" });
                    //                    this.put(5, new Object[] { DestinationFormat.VIDEOFLV, "H.263", "MP3", "Stereo" });
                    //                    this.put(6, new Object[] { DestinationFormat.VIDEOFLV, "H.263", "MP3", "Mono" });
                    //                }
                    //                if (flv) {
                    //                    this.put(34, new Object[] { DestinationFormat.VIDEOFLV, "H.264", "AAC", "Stereo" });
                    //                    this.put(35, new Object[] { DestinationFormat.VIDEOFLV, "H.264", "AAC", "Stereo" });
                    //                }

                    // **** 3GP *****
                    //                if (threegp) {
                    //                    this.put(13, new Object[] { DestinationFormat.VIDEO3GP, "H.263", "AMR", "Mono" });
                    //                    this.put(17, new Object[] { DestinationFormat.VIDEO3GP, "H.264", "AAC", "Stereo" });
                    //                }
                    // **** MP4 *****
                    if (mp4) {
                        this.put(18, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo" });
                        this.put(22, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo" });
                        this.put(37, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo" });
                        //this.put(38, new Object[] { DestinationFormat.VIDEOMP4, "H.264", "AAC", "Stereo" });
                    }
                    // **** WebM *****
                    //                if (webm) {
                    //                    this.put(43, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo" });
                    //                    this.put(45, new Object[] { DestinationFormat.VIDEOWEBM, "VP8", "Vorbis", "Stereo" });
                    //                }
                }
            };

            /* check for wished formats first */
            String dlLink = "";
            String vQuality = "";
            DestinationFormat cMode = null;

            for (final Integer format : LinksFound.keySet()) {
                if (ytVideo.containsKey(format)) {
                    cMode = (DestinationFormat) ytVideo.get(format)[0];
                    vQuality = "(" + LinksFound.get(format)[1] + "_" + ytVideo.get(format)[1] + "-" + ytVideo.get(format)[2] + ")";
                } else {
                    cMode = DestinationFormat.UNKNOWN;
                    vQuality = "(" + LinksFound.get(format)[1] + "_" + format + ")";
                    /*
                     * we do not want to download unknown formats at the
                     * moment
                     */
                    continue;
                }
                dlLink = LinksFound.get(format)[0];
                try {
                    if (fast) {
                        this.addtopos(cMode, dlLink, 0, vQuality, format);
                    } else if (br.openGetConnection(dlLink).getResponseCode() == 200) {
                        this.addtopos(cMode, dlLink, br.getHttpConnection().getLongContentLength(), vQuality, format);
                    }
                } catch (final Throwable e) {
                    Log.e(TAG, "Error in youtube decrypt logic", e);
                } finally {
                    try {
                        br.getHttpConnection().disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }

            int lastFmt = 0;
            for (final Entry<DestinationFormat, ArrayList<Info>> next : this.possibleconverts.entrySet()) {
                final DestinationFormat convertTo = next.getKey();
                for (final Info info : next.getValue()) {
                    String link = info.link;
                    //thislink.setBrowserUrl(parameter);
                    //thislink.setFinalFileName(YT_FILENAME + info.desc + convertTo.getExtFirst());
                    long size = info.size;
                    String name = null;
                    if (convertTo != DestinationFormat.AUDIOMP3) {
                        name = YT_FILENAME + info.desc + convertTo.getExtFirst();
                        name = FileUtils.getValidFileName(name);
                    } else {
                        /*
                         * because demuxer will fail when mp3 file already
                         * exists
                         */
                        //name = YT_FILENAME + info.desc + ".tmp";
                        //thislink.setProperty("name", name);
                    }
                    //thislink.setProperty("convertto", convertTo.name());
                    //thislink.setProperty("videolink", parameter);
                    //thislink.setProperty("valid", true);
                    //thislink.setProperty("fmtNew", info.fmt);
                    //thislink.setProperty("LINKDUPEID", name);

                    if (lastFmt < info.fmt) {
                        decryptedLink = new HttpDownloadLink(link, name, FilenameUtils.getBaseName(name), size, false);
                    }
                }
            }
        } catch (final IOException e) {
            br.getHttpConnection().disconnect();
            //logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            return null;
        }

        return decryptedLink;
    }

    private HashMap<Integer, String[]> getLinks(final String video, final boolean prem, Browser br, int retrycount) throws Exception {
        if (retrycount > 2) {
            // do not retry more often than 2 time
            return null;
        }
        //        if (br == null) {
        //            br = this.br;
        //        }
        br.setFollowRedirects(true);
        /* this cookie makes html5 available and skip controversy check */
        br.setCookie("youtube.com", "PREF", "f2=40100000");
        br.getHeaders().put("User-Agent", "Wget/1.12");
        br.getPage(video);
        if (br.containsHTML("id=\"unavailable-submessage\" class=\"watch-unavailable-submessage\"")) {
            return null;
        }
        final String VIDEOID = new Regex(video, "watch\\?v=([\\w_\\-]+)").getMatch(0);
        boolean fileNameFound = false;
        String YT_FILENAME = VIDEOID;
        if (br.containsHTML("&title=")) {
            YT_FILENAME = Encoding.htmlDecode(br.getRegex("&title=([^&$]+)").getMatch(0).replaceAll("\\+", " ").trim());
            fileNameFound = true;
        }
        final String url = br.getURL();
        boolean ythack = false;
        if (url != null && !url.equals(video)) {
            /* age verify with activated premium? */
            //            if (url.toLowerCase(Locale.ENGLISH).indexOf("youtube.com/verify_age?next_url=") != -1) {
            //                verifyAge = true;
            //            }
            if (url.toLowerCase(Locale.ENGLISH).indexOf("youtube.com/verify_age?next_url=") != -1 && prem) {
                final String session_token = br.getRegex("onLoadFunc.*?gXSRF_token = '(.*?)'").getMatch(0);
                final LinkedHashMap<String, String> p = Request.parseQuery(url);
                final String next = p.get("next_url");
                final Form form = new Form();
                form.setAction(url);
                form.setMethod(MethodType.POST);
                form.put("next_url", "%2F" + next.substring(1));
                form.put("action_confirm", "Confirm+Birth+Date");
                form.put("session_token", Encoding.urlEncode(session_token));
                br.submitForm(form);
                if (br.getCookie("http://www.youtube.com", "is_adult") == null) {
                    return null;
                }
            } else if (url.toLowerCase(Locale.ENGLISH).indexOf("youtube.com/index?ytsession=") != -1 || url.toLowerCase(Locale.ENGLISH).indexOf("youtube.com/verify_age?next_url=") != -1 && !prem) {
                ythack = true;
                br.getPage("http://www.youtube.com/get_video_info?video_id=" + VIDEOID);
                if (br.containsHTML("&title=") && fileNameFound == false) {
                    YT_FILENAME = Encoding.htmlDecode(br.getRegex("&title=([^&$]+)").getMatch(0).replaceAll("\\+", " ").trim());
                    fileNameFound = true;
                }
            } else if (url.toLowerCase(Locale.ENGLISH).indexOf("google.com/accounts/servicelogin?") != -1) {
                // private videos
                return null;
            }
        }
        /* html5_fmt_map */
        if (br.getRegex(YT_FILENAME_PATTERN).count() != 0 && fileNameFound == false) {
            YT_FILENAME = Encoding.htmlDecode(br.getRegex(YT_FILENAME_PATTERN).getMatch(0).trim());
            fileNameFound = true;
        }
        final HashMap<Integer, String[]> links = new HashMap<Integer, String[]>();
        String html5_fmt_map = br.getRegex("\"html5_fmt_map\": \\[(.*?)\\]").getMatch(0);

        if (html5_fmt_map != null) {
            String[] html5_hits = new Regex(html5_fmt_map, "\\{(.*?)\\}").getColumn(0);
            if (html5_hits != null) {
                for (String hit : html5_hits) {
                    String hitUrl = new Regex(hit, "url\": \"(http:.*?)\"").getMatch(0);
                    String hitFmt = new Regex(hit, "itag\": (\\d+)").getMatch(0);
                    String hitQ = new Regex(hit, "quality\": \"(.*?)\"").getMatch(0);
                    if (hitUrl != null && hitFmt != null && hitQ != null) {
                        hitUrl = unescape(hitUrl.replaceAll("\\\\/", "/"));
                        links.put(Integer.parseInt(hitFmt), new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, true)), hitQ });
                    }
                }
            }
        } else {
            /* new format since ca. 1.8.2011 */
            html5_fmt_map = br.getRegex("\"url_encoded_fmt_stream_map\": \"(.*?)\"").getMatch(0);
            if (html5_fmt_map == null) {
                html5_fmt_map = br.getRegex("url_encoded_fmt_stream_map=(.*?)(&|$)").getMatch(0);
                if (html5_fmt_map != null) {
                    html5_fmt_map = html5_fmt_map.replaceAll("%2C", ",");
                    if (!html5_fmt_map.contains("url=")) {
                        html5_fmt_map = html5_fmt_map.replaceAll("%3D", "=");
                        html5_fmt_map = html5_fmt_map.replaceAll("%26", "&");
                    }
                }
            }
            if (html5_fmt_map != null && !html5_fmt_map.contains("signature") && !html5_fmt_map.contains("sig")) {
                Thread.sleep(5000);
                br.clearCookies("youtube.com");
                return getLinks(video, prem, br, retrycount + 1);
            }
            if (html5_fmt_map != null) {
                String[] html5_hits = new Regex(html5_fmt_map, "(.*?)(,|$)").getColumn(0);
                if (html5_hits != null) {
                    for (String hit : html5_hits) {
                        hit = unescape(hit);
                        String hitUrl = new Regex(hit, "url=(http.*?)(\\&|$)").getMatch(0);
                        String sig = new Regex(hit, "url=http.*?(\\&|$)(sig|signature)=(.*?)(\\&|$)").getMatch(2);
                        String hitFmt = new Regex(hit, "itag=(\\d+)").getMatch(0);
                        String hitQ = new Regex(hit, "quality=(.*?)(\\&|$)").getMatch(0);
                        if (hitUrl != null && hitFmt != null && hitQ != null) {
                            hitUrl = unescape(hitUrl.replaceAll("\\\\/", "/"));
                            if (hitUrl.startsWith("http%253A")) {
                                hitUrl = Encoding.htmlDecode(hitUrl);
                            }
                            String[] inst = null;
                            if (hitUrl.contains("sig")) {
                                inst = new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, true)), hitQ };
                            } else {
                                inst = new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, true) + "&signature=" + sig), hitQ };
                            }
                            links.put(Integer.parseInt(hitFmt), inst);
                        }
                    }
                }
            }
        }

        /* normal links */
        final HashMap<String, String> fmt_list = new HashMap<String, String>();
        String fmt_list_str = "";
        if (ythack) {
            fmt_list_str = (br.getMatch("&fmt_list=(.+?)&") + ",").replaceAll("%2F", "/").replaceAll("%2C", ",");
        } else {
            fmt_list_str = (br.getMatch("\"fmt_list\":\\s+\"(.+?)\",") + ",").replaceAll("\\\\/", "/");
        }
        final String fmt_list_map[][] = new Regex(fmt_list_str, "(\\d+)/(\\d+x\\d+)/\\d+/\\d+/\\d+,").getMatches();
        for (final String[] fmt : fmt_list_map) {
            fmt_list.put(fmt[0], fmt[1]);
        }
        if (links.size() == 0 && ythack) {
            /* try to find fallback links */
            String urls[] = br.getRegex("url%3D(.*?)($|%2C)").getColumn(0);
            int index = 0;
            for (String vurl : urls) {
                String hitUrl = new Regex(vurl, "(.*?)%26").getMatch(0);
                String hitQ = new Regex(vurl, "%26quality%3D(.*?)%").getMatch(0);
                if (hitUrl != null && hitQ != null) {
                    hitUrl = unescape(hitUrl.replaceAll("\\\\/", "/"));
                    if (fmt_list_map.length >= index) {
                        links.put(Integer.parseInt(fmt_list_map[index][0]), new String[] { Encoding.htmlDecode(Encoding.urlDecode(hitUrl, false)), hitQ });
                        index++;
                    }
                }
            }
        }
        for (Integer fmt : links.keySet()) {
            String fmt2 = fmt + "";
            if (fmt_list.containsKey(fmt2)) {
                String Videoq = links.get(fmt)[1];
                final Integer q = Integer.parseInt(fmt_list.get(fmt2).split("x")[1]);
                if (fmt == 40) {
                    Videoq = "240p Light";
                } else if (q > 1080) {
                    Videoq = "Original";
                } else if (q > 720) {
                    Videoq = "1080p";
                } else if (q > 576) {
                    Videoq = "720p";
                } else if (q > 360) {
                    Videoq = "480p";
                } else if (q > 240) {
                    Videoq = "360p";
                } else {
                    Videoq = "240p";
                }
                links.get(fmt)[1] = Videoq;
            }
        }
        if (YT_FILENAME != null) {
            links.put(-1, new String[] { YT_FILENAME });
        }
        return links;
    }

    private void addtopos(final DestinationFormat mode, final String link, final long size, final String desc, final int fmt) {
        ArrayList<Info> info = this.possibleconverts.get(mode);
        if (info == null) {
            info = new ArrayList<Info>();
            this.possibleconverts.put(mode, info);
        }
        final Info tmp = new Info();
        tmp.link = link;
        tmp.size = size;
        tmp.desc = desc;
        tmp.fmt = fmt;
        info.add(tmp);
    }

    private static String unescape(final String s) {
        char ch;
        char ch2;
        final StringBuilder sb = new StringBuilder();
        int ii;
        int i;
        for (i = 0; i < s.length(); i++) {
            ch = s.charAt(i);
            switch (ch) {
            case '%':
            case '\\':
                ch2 = ch;
                ch = s.charAt(++i);
                StringBuilder sb2 = null;
                switch (ch) {
                case 'u':
                    /* unicode */
                    sb2 = new StringBuilder();
                    i++;
                    ii = i + 4;
                    for (; i < ii; i++) {
                        ch = s.charAt(i);
                        if (sb2.length() > 0 || ch != '0') {
                            sb2.append(ch);
                        }
                    }
                    i--;
                    sb.append((char) Long.parseLong(sb2.toString(), 16));
                    continue;
                case 'x':
                    /* normal hex coding */
                    sb2 = new StringBuilder();
                    i++;
                    ii = i + 2;
                    for (; i < ii; i++) {
                        ch = s.charAt(i);
                        sb2.append(ch);
                    }
                    i--;
                    sb.append((char) Long.parseLong(sb2.toString(), 16));
                    continue;
                default:
                    if (ch2 == '%') {
                        sb.append(ch2);
                    }
                    sb.append(ch);
                    continue;
                }

            }
            sb.append(ch);
        }

        return sb.toString();
    }

    public static enum DestinationFormat {
        AUDIOMP3("Audio (MP3)", new String[] { ".mp3" }), VIDEOFLV("Video (FLV)", new String[] { ".flv" }), VIDEOMP4("Video (MP4)", new String[] { ".mp4" }), VIDEOWEBM("Video (Webm)", new String[] { ".webm" }), VIDEO3GP("Video (3GP)", new String[] { ".3gp" }), UNKNOWN("Unknown (unk)",
                new String[] { ".unk" }),

        VIDEOIPHONE("Video (IPhone)", new String[] { ".mp4" });

        private String text;
        private String[] ext;

        DestinationFormat(final String text, final String[] ext) {
            this.text = text;
            this.ext = ext;
        }

        public String getExtFirst() {
            return this.ext[0];
        }

        public String getText() {
            return this.text;
        }

        @Override
        public String toString() {
            return this.text;
        }

    }

    static class Info {
        public String link;
        public long size;
        public int fmt;
        public String desc;
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
            Movie inVideo = MovieCreator.build(inFC);

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
