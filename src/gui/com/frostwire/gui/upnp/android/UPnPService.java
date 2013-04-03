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

package com.frostwire.gui.upnp.android;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.fourthline.cling.DefaultUpnpServiceConfiguration.ClingThreadFactory;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidNetworkAddressFactory;
import org.fourthline.cling.android.AndroidRouter;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.UnsupportedDataException;
import org.fourthline.cling.model.message.IncomingDatagramMessage;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.DatagramIOConfigurationImpl;
import org.fourthline.cling.transport.impl.DatagramIOImpl;
import org.fourthline.cling.transport.impl.DatagramProcessorImpl;
import org.fourthline.cling.transport.impl.MulticastReceiverConfigurationImpl;
import org.fourthline.cling.transport.impl.MulticastReceiverImpl;
import org.fourthline.cling.transport.spi.DatagramIO;
import org.fourthline.cling.transport.spi.DatagramProcessor;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.MulticastReceiver;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;

import android.content.Context;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class UPnPService extends AndroidUpnpServiceImpl {

    private static Logger log = Logger.getLogger(UPnPService.class.getName());

    private static final int REGISTRY_MAINTENANCE_INTERVAL_MILLIS = 5000; // 5 seconds

    private long lastTimeIncomingSearchRequestParsed = -1;

    private final int INCOMING_SEARCH_REQUEST_PARSE_INTERVAL = 2500;

    private Map<String, Long> readResponseWindows = new LinkedHashMap<String, Long>();

    @Override
    protected AndroidUpnpServiceConfiguration createConfiguration() {
        return new AndroidUpnpServiceConfiguration() {

            @Override
            public int getRegistryMaintenanceIntervalMillis() {
                return REGISTRY_MAINTENANCE_INTERVAL_MILLIS;
            }

            @Override
            public ServiceType[] getExclusiveServiceTypes() {
                return new ServiceType[] { new UDAServiceType("UPnPFWDeviceInfo") };
            }

            @Override
            protected ExecutorService createDefaultExecutorService() {
                return createFrostWireExecutor();
            }

            @Override
            protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort) {
                return new AndroidNetworkAddressFactory(streamListenPort) {

                    private byte[] addressCached = null;

                    @Override
                    public byte[] getHardwareAddress(InetAddress inetAddress) {
                        if (addressCached == null) {
                            // ignoring synchronization issues since it's not a big deal here
                            // also, assuming that the inetAddress (the preferred one) is always the same
                            addressCached = super.getHardwareAddress(inetAddress);
                        }
                        return addressCached;
                    }
                };
            }

            @Override
            protected DatagramProcessor createDatagramProcessor() {
                return new DatagramProcessorImpl() {

                    private final long WAIT_TIME = 8000;
                    private final long WINDOW_SIZE = 1000;

                    @Override
                    protected IncomingDatagramMessage readRequestMessage(InetAddress receivedOnAddress, DatagramPacket datagram, ByteArrayInputStream is, String requestMethod, String httpProtocol) throws Exception {
                        //Throttle the parsing of incoming search messages.
                        if (UpnpRequest.Method.getByHttpName(requestMethod).equals(UpnpRequest.Method.MSEARCH)) {
                            if (System.currentTimeMillis() - lastTimeIncomingSearchRequestParsed < INCOMING_SEARCH_REQUEST_PARSE_INTERVAL) {
                                return null;
                            } else {
                                lastTimeIncomingSearchRequestParsed = System.currentTimeMillis();
                            }
                        }

                        return super.readRequestMessage(receivedOnAddress, datagram, is, requestMethod, httpProtocol);
                    }

                    @Override
                    protected IncomingDatagramMessage readResponseMessage(InetAddress receivedOnAddress, DatagramPacket datagram, ByteArrayInputStream is, int statusCode, String statusMessage, String httpProtocol) throws Exception {

                        IncomingDatagramMessage response = null;
                        String host = datagram.getAddress().getHostAddress();

                        if (!readResponseWindows.containsKey(host)) {
                            response = super.readResponseMessage(receivedOnAddress, datagram, is, statusCode, statusMessage, httpProtocol);
                            readResponseWindows.put(host, System.currentTimeMillis());

                        } else {
                            long windowStart = readResponseWindows.get(host);
                            long delta = System.currentTimeMillis() - windowStart;
                            if (delta >= 0 && delta < WINDOW_SIZE) {
                                response = super.readResponseMessage(receivedOnAddress, datagram, is, statusCode, statusMessage, httpProtocol);
                            } else if ((System.currentTimeMillis() - windowStart > (2 * WINDOW_SIZE) / 3)) {
                                readResponseWindows.put(host, System.currentTimeMillis() + WAIT_TIME);
                            } else {
                                //System.out.println("Come back later " + host + " !!!");
                            }
                        }

                        return response;
                    }
                };
            }

            public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
                return new DatagramIOImpl(new DatagramIOConfigurationImpl()) {
                    public void run() {
                        while (true) {
                            try {
                                byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
                                DatagramPacket datagram = new DatagramPacket(buf, buf.length);
                                socket.receive(datagram);
                                IncomingDatagramMessage incomingDatagramMessage = datagramProcessor.read(localAddress.getAddress(), datagram);

                                if (incomingDatagramMessage != null) {
                                    router.received(incomingDatagramMessage);
                                }

                            } catch (SocketException ex) {
                                log.fine("Socket closed");
                                break;
                            } catch (UnsupportedDataException ex) {
                                log.info("Could not read datagram: " + ex.getMessage());
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                        try {
                            if (!socket.isClosed()) {
                                log.fine("Closing unicast socket");
                                socket.close();
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
            }

            public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
                return new MulticastReceiverImpl(new MulticastReceiverConfigurationImpl(networkAddressFactory.getMulticastGroup(), networkAddressFactory.getMulticastPort())) {
                    public void run() {
                        while (true) {
                            try {
                                byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
                                DatagramPacket datagram = new DatagramPacket(buf, buf.length);

                                socket.receive(datagram);

                                InetAddress receivedOnLocalAddress = networkAddressFactory.getLocalAddress(multicastInterface, multicastAddress.getAddress() instanceof Inet6Address, datagram.getAddress());

                                IncomingDatagramMessage incomingDatagramMessage = datagramProcessor.read(receivedOnLocalAddress, datagram);

                                if (incomingDatagramMessage != null) {
                                    router.received(incomingDatagramMessage);
                                }

                            } catch (SocketException ex) {
                                log.info("Socket closed");
                                break;
                            } catch (UnsupportedDataException ex) {
                                log.info("Could not read datagram: " + ex.getMessage());
                                ex.printStackTrace();
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                        try {
                            if (!socket.isClosed()) {
                                log.info("Closing multicast socket");
                                socket.close();
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
            }
        };
    }

    protected ExecutorService createFrostWireExecutor() {
        return new ThreadPoolExecutor(0, 32, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ClingThreadFactory()) {
            @Override
            public void execute(Runnable command) {
                try {
                    super.execute(command);
                } catch (Throwable e) {
                    //gubatron: we're catching a RejectedExecutionException until we figure out a solution.
                    //we're probably being too aggresive submitting tasks in the first place.
                }
            }
        };
    }

    @Override
    protected AndroidRouter createRouter(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory, Context context) {
        return new AndroidRouter(configuration, protocolFactory, context);
    }

}