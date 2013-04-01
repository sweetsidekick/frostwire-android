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

import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.fourthline.cling.DefaultUpnpServiceConfiguration.ClingThreadFactory;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidRouter;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.UnsupportedDataException;
import org.fourthline.cling.model.message.IncomingDatagramMessage;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.message.discovery.IncomingSearchRequest;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.protocol.ProtocolFactory;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.DatagramIOConfigurationImpl;
import org.fourthline.cling.transport.impl.DatagramIOImpl;
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
    
    private static final int DATAGRAM_RECEIVER_THROTTLE_PAUSE = 4000;

    private static Logger log = Logger.getLogger(UPnPService.class.getName());

    private static final int REGISTRY_MAINTENANCE_INTERVAL_MILLIS = 5000; // 5 seconds

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
            
            public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
                return new DatagramIOImpl(new DatagramIOConfigurationImpl()) {
                    public void run() {
                        //log.fine("Entering blocking receiving loop, listening for UDP datagrams on: " + socket.getLocalAddress());
                        
                        int rate = DATAGRAM_RECEIVER_THROTTLE_PAUSE/1000;
                        
                        while (true) {
                            
                            
                            try {
                                byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
                                DatagramPacket datagram = new DatagramPacket(buf, buf.length);

                                socket.receive(datagram);

                                /*
                                log.fine(
                                        "UDP datagram received from: "
                                                + datagram.getAddress().getHostAddress()
                                                + ":" + datagram.getPort()
                                                + " on: " + localAddress
                                );
                                */

                                IncomingDatagramMessage incomingDatagramMessage = datagramProcessor.read(localAddress.getAddress(), datagram);
                                
                                //if (! (incomingDatagramMessage instanceof IncomingSearchRequest)) {
                                    router.received(incomingDatagramMessage);
                                //}

                            } catch (SocketException ex) {
                                log.fine("Socket closed");
                                break;
                            } catch (UnsupportedDataException ex) {
                                log.info("Could not read datagram: " + ex.getMessage());
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                            
                            /**
                            try {
                                //Throttles datagram parsing for less CPU usage
                                if (rate == 0) {
                                    //Thread.sleep(DATAGRAM_RECEIVER_THROTTLE_PAUSE);
                                    rate = DATAGRAM_RECEIVER_THROTTLE_PAUSE/1000;
                                } else {
                                    rate--;
                                }
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            */
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

                    private MulticastSocket socket;
                    
                    synchronized public void init(NetworkInterface networkInterface, Router router, NetworkAddressFactory networkAddressFactory, DatagramProcessor datagramProcessor)
                            throws InitializationException {

                        this.router = router;
                        this.networkAddressFactory = networkAddressFactory;
                        this.datagramProcessor = datagramProcessor;
                        this.multicastInterface = networkInterface;

                        try {

                            log.info("Creating wildcard socket (for receiving multicast datagrams) on port: " + configuration.getPort());
                            multicastAddress = new InetSocketAddress(configuration.getGroup(), configuration.getPort());

                            socket = new MulticastSocket(configuration.getPort());
                            socket.setReuseAddress(true);
                            socket.setReceiveBufferSize(32768); // Keep a backlog of incoming datagrams if we are not fast enough

                            log.info("Joining multicast group: " + multicastAddress + " on network interface: " + multicastInterface.getDisplayName());
                            socket.joinGroup(multicastAddress, multicastInterface);

                        } catch (Exception ex) {
                            throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex);
                        }
                    }

                    public void run() {
                        int rate = DATAGRAM_RECEIVER_THROTTLE_PAUSE/1000;
                        //log.fine("Entering blocking receiving loop, listening for UDP datagrams on: " + socket.getLocalAddress());
                        while (true) {
                            try {
                                byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
                                DatagramPacket datagram = new DatagramPacket(buf, buf.length);

                                socket.receive(datagram);

                                InetAddress receivedOnLocalAddress = networkAddressFactory.getLocalAddress(multicastInterface, multicastAddress.getAddress() instanceof Inet6Address,
                                        datagram.getAddress());

                                //log.info("UDP datagram received from: " + datagram.getAddress().getHostAddress() + ":" + datagram.getPort() + " on local interface: "
                                //        + multicastInterface.getDisplayName() + " and address: " + receivedOnLocalAddress.getHostAddress());

//                                router.received(datagramProcessor.read(receivedOnLocalAddress, datagram));
                                IncomingDatagramMessage incomingDatagramMessage = datagramProcessor.read(receivedOnLocalAddress, datagram);
                                
                                //if (incomingDatagramMessage.getOperation() instanceof UpnpRequest) {
                                //    IncomingDatagramMessage<UpnpRequest> incomingRequest = incomingDatagramMessage;

                                //    if (incomingRequest.getOperation().getMethod() != UpnpRequest.Method.MSEARCH) {
                                        router.received(incomingDatagramMessage);
                                 //   }
                                    
                                //}

                            } catch (SocketException ex) {
                                log.info("Socket closed");
                                break;
                            } catch (UnsupportedDataException ex) {
                                log.info("Could not read datagram: " + ex.getMessage());
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                            
                            /**
                            try {
                                //Throttles datagram parsing for less CPU usage
                                if (rate == 0) {
                                    Thread.sleep(DATAGRAM_RECEIVER_THROTTLE_PAUSE);
                                    rate = DATAGRAM_RECEIVER_THROTTLE_PAUSE/1000;
                                } else {
                                    rate--;
                                }
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            */
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