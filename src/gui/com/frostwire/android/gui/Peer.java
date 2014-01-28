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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.List;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.core.HttpFetcher;
import com.frostwire.localpeer.Finger;
import com.frostwire.util.JsonUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class Peer implements Cloneable {

    private static final int BROWSE_HTTP_TIMEOUT = 10000;

    private String udn;
    private InetAddress address;
    private int listeningPort;

    /**
     * 16 bytes (128bit - UUID identifier letting us know who is the sender)
     */
    private String uuid;
    private String nickname;
    private int numSharedFiles;
    private String clientVersion;
    private int deviceMajorType;

    private int hashCode = -1;
    private boolean localhost;

    public Peer() {
    }

    public Peer(String udn, InetAddress address, PingInfo p) {
        this.udn = udn;
        this.address = address;
        this.listeningPort = p.listeningPort;

        this.setUUID(p.uuid);

        this.nickname = p.nickname;
        this.numSharedFiles = p.numSharedFiles;
        this.deviceMajorType = p.deviceMajorType;
        this.clientVersion = p.clientVersion;
    }

    public String getUdn() {
        return udn;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public String getUUID() {
        return uuid;
    }

    void setUUID(String uuid) {
        this.uuid = uuid;

        this.hashCode = this.uuid.hashCode();
        this.localhost = this.uuid.equals(ConfigurationManager.instance().getUUIDString());
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getNumSharedFiles() {
        return numSharedFiles;
    }

    public int getDeviceMajorType() {
        return deviceMajorType;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public boolean isLocalHost() {
        return localhost;
    }

    public String getFingerUri() {
        return "http://" + address.getHostAddress() + ":" + listeningPort + "/finger";
    }

    public String getBrowseUri(byte fileType) {
        return "http://" + address.getHostAddress() + ":" + listeningPort + "/browse?type=" + fileType;
    }

    public String getDownloadUri(FileDescriptor fd) {
        return "http://" + address.getHostAddress() + ":" + listeningPort + "/download?type=" + fd.fileType + "&id=" + fd.id;
    }

    public Finger finger() {
        if (localhost) {
            return Librarian.instance().finger(localhost);
        } else {
            String uri = getFingerUri();
            byte[] data = new HttpFetcher(uri).fetch();
            String json = new String(data);
            return JsonUtils.toObject(json, Finger.class);
        }
    }

    public List<FileDescriptor> browse(byte fileType) {
        if (localhost) {
            return Librarian.instance().getFiles(fileType, 0, Integer.MAX_VALUE, false);
        } else {
            String uri = getBrowseUri(fileType);
            byte[] data = new HttpFetcher(uri, BROWSE_HTTP_TIMEOUT).fetchGzip();
            String json;
            try {
                json = new String(data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                json = new String(data);
            }
            return JsonUtils.toObject(json, FileDescriptorList.class).files;
        }
    }

    @Override
    public String toString() {
        return "Peer(" + nickname + "@" + (address != null ? address.getHostAddress() : "unknown") + ", v:" + clientVersion + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Peer)) {
            return false;
        }

        return hashCode() == ((Peer) o).hashCode();
    }

    @Override
    public int hashCode() {
        return this.hashCode != -1 ? this.hashCode : super.hashCode();
    }

    @Override
    public Peer clone() {
        Peer peer = new Peer();

        peer.address = this.address;
        peer.listeningPort = listeningPort;
        peer.setUUID(this.uuid);
        peer.nickname = this.nickname;
        peer.numSharedFiles = this.numSharedFiles;
        peer.clientVersion = this.clientVersion;

        return peer;
    }

    private static final class FileDescriptorList {
        public List<FileDescriptor> files;
    }
}