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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.support.v4.util.LruCache;
import android.util.Log;

import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.httpserver.HttpServerManager;
import com.frostwire.localpeer.AndroidMulticastLock;
import com.frostwire.localpeer.LocalPeer;
import com.frostwire.localpeer.LocalPeerManager;
import com.frostwire.localpeer.LocalPeerManagerImpl;
import com.frostwire.localpeer.LocalPeerManagerListener;

/**
 * Keeps track of the Peers we know.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class PeerManager {

    private static final String TAG = "FW.PeerManager";

    private final int maxPeers;
    private final LruCache<Peer, Peer> peerCache;
    private final Map<String, Peer> addressMap;

    private final PeerComparator peerComparator;

    private static PeerManager instance;

    private final LocalPeerManager peerManager;
    private final HttpServerManager httpServerManager;

    public static PeerManager instance() {
        if (instance == null) {
            instance = new PeerManager();
        }
        return instance;
    }

    private PeerManager() {
        this.maxPeers = Constants.PEER_MANAGER_MAX_PEERS;
        //this.cacheTimeout = Constants.PEER_MANAGER_CACHE_TIMEOUT;
        this.peerCache = new LruCache<Peer, Peer>(maxPeers);
        this.addressMap = new HashMap<String, Peer>();

        this.peerComparator = new PeerComparator();

        this.peerManager = new LocalPeerManagerImpl(new AndroidMulticastLock(NetworkManager.instance().getWifiManager()));
        this.peerManager.setListener(new LocalPeerManagerListener() {

            @Override
            public void peerResolved(LocalPeer peer) {
                onMessageReceived(peer, true);
            }

            @Override
            public void peerRemoved(LocalPeer peer) {
                onMessageReceived(peer, false);
            }
        });

        this.httpServerManager = new HttpServerManager();
    }

    public Peer getLocalPeer() {
        return new Peer(createLocalPeer(), true);
    }

    public void onMessageReceived(LocalPeer p, boolean added) {
        if (p != null) {
            Peer peer = new Peer(p, p.local);

            updatePeerCache2(peer, !added);
        }
    }

    /**
     * This returns a shadow-copy of the peer cache as an ArrayList plus the local peer.
     * 
     * @return
     */
    public List<Peer> getPeers() {
        List<Peer> peers = new ArrayList<Peer>(1 + peerCache.size());

        for (Peer p : peerCache.snapshot().values()) {
            peers.add(p);
        }

        Collections.sort(peers, peerComparator);

        return peers;
    }

    /**
     * @param uuid
     * @return
     */
    public Peer findPeerByKey(String key) {
        if (key == null) {
            return null;
        }

        Peer p = addressMap.get(key);

        return p;
    }

    public void clear() {
        peerCache.evictAll();
    }

    public void removePeer(Peer p) {
        try {
            updatePeerCache2(p, true);
        } catch (Throwable e) {
            Log.e(TAG, "Error removing peer from manager", e);
        }
    }

    public void start() {
        httpServerManager.start(NetworkManager.instance().getListeningPort());
        peerManager.start(createLocalPeer());
    }

    public void stop() {
        httpServerManager.stop();
        peerManager.stop();
    }

    public void updateLocalPeer() {
        peerManager.update(createLocalPeer());
    }

    private LocalPeer createLocalPeer() {
        String address = "0.0.0.0";
        int port = NetworkManager.instance().getListeningPort();
        int numSharedFiles = Librarian.instance().getNumFiles();
        String nickname = ConfigurationManager.instance().getNickname();
        String clientVersion = Constants.FROSTWIRE_VERSION_STRING;
        int deviceType = Constants.DEVICE_MAJOR_TYPE_PHONE;

        return new LocalPeer(address, port, true, nickname, numSharedFiles, deviceType, clientVersion);
    }

    private void updatePeerCache2(Peer peer, boolean disconnected) {
        if (disconnected) {
            Peer p = addressMap.remove(peer.getKey());
            if (p != null) {
                peerCache.remove(p);
            }
        } else {
            addressMap.put(peer.getKey(), peer);
            updatePeerCache(peer, disconnected);
        }
    }

    /**
     * Invoke this method whenever you have new information about a peer. For
     * now we invoke this whenever we receive a ping.
     * 
     * @param peer
     * @param disconnected
     */
    private void updatePeerCache(Peer peer, boolean disconnected) {
        // first time we hear from a peer
        if (peerCache.get(peer) == null) {
            // no more ghosts...
            if (disconnected) {
                return;
            }

            // there's no room
            boolean cacheFull = peerCache.size() >= maxPeers;

            // add it to the peer cache
            if (!cacheFull) {
                peerCache.put(peer, peer);
                Log.v(TAG, String.format("Adding new peer, total=%s: %s", peerCache.size(), peer));
            }
        } else {
            if (!disconnected) {
                peerCache.put(peer, peer); // touch the element and updates the properties
            } else {
                peerCache.remove(peer);
            }
        }
    }

    private static final class PeerComparator implements Comparator<Peer> {
        public int compare(Peer lhs, Peer rhs) {
            int c = lhs.getNickname().compareTo(rhs.getNickname());
            if (c == 0) {
                c = rhs.hashCode() - lhs.hashCode();
            }
            return c;
        }
    }
}