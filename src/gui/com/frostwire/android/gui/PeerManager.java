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

import java.net.InetAddress;
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
import com.frostwire.gui.upnp.PingInfo;

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

    private Peer localPeer;

    private static PeerManager instance;

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

        refreshLocalPeer();
    }

    public Peer getLocalPeer() {
        return localPeer;
    }

    public void onMessageReceived(String udn, InetAddress address, boolean added, PingInfo p) {
        if (p != null) {
            Peer peer = new Peer(udn, address, p);

            if (!peer.isLocalHost()) {
                updatePeerCache2(udn, peer, !added);
            }
        } else {
            Peer peer = addressMap.remove(udn);
            if (peer != null) {
                peerCache.remove(peer);
            }
        }
    }

    /**
     * This returns a shadow-copy of the peer cache as an ArrayList plus the local peer.
     * 
     * @return
     */
    public List<Peer> getPeers() {
        refreshLocalPeer();
        List<Peer> peers = new ArrayList<Peer>(1 + peerCache.size());

        for (Peer p : peerCache.snapshot().values()) {
            peers.add(p);
        }

        Collections.sort(peers, peerComparator);
        peers.add(0, localPeer);

        return peers;
    }

    /**
     * @param uuid
     * @return
     */
    public Peer findPeerByUUID(String uuid) {
        if (uuid == null) {
            return null;
        }

        if (uuid.equals(ConfigurationManager.instance().getUUIDString())) {
            return localPeer;
        }

        Peer k = new Peer();
        k.setUUID(uuid);

        Peer p = peerCache.get(k);

        return p;
    }

    public void clear() {
        refreshLocalPeer();
        peerCache.evictAll();
    }

    public void removePeer(Peer p) {
        try {
            updatePeerCache2(p.getUdn(), p, true);
            //UPnPManager.instance().getService().getRegistry().removeDevice(UDN.valueOf(p.getUdn()));
        } catch (Throwable e) {
            Log.e(TAG, "Error removing peer from manager", e);
        }
    }

    private void updatePeerCache2(String udn, Peer peer, boolean disconnected) {
        if (disconnected) {
            Peer p = addressMap.remove(udn);
            if (p != null) {
                peerCache.remove(p);
            }
        } else {
            addressMap.put(udn, peer);
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

    private void refreshLocalPeer() {
        PingInfo p = new PingInfo();//UPnPManager.instance().getLocalPingInfo();
        p.uuid = ConfigurationManager.instance().getUUIDString();
        p.nickname = ConfigurationManager.instance().getNickname();
        p.clientVersion = Constants.FROSTWIRE_VERSION_STRING;

        localPeer = new Peer(ConfigurationManager.instance().getUUIDString(), null, p);
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