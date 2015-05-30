/*
 * Copyright (C) 2015 tkv
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
package org.vesalainen.comm.channel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import static java.net.StandardSocketOptions.IP_MULTICAST_LOOP;
import static java.net.StandardSocketOptions.SO_BROADCAST;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * @author tkv
 */
public class SimpleSync implements AutoCloseable
{
    private int port;
    private final DatagramChannel dc;
    private byte phase;
    private final ByteBuffer bb = ByteBuffer.allocateDirect(10);
    private final Selector selector;
    private final SelectionKey selectionKey;
    private final InetSocketAddress broadcast;
    private final List<InetAddress> locals = new ArrayList<>();
    private SimpleSync(int port, DatagramChannel dc) throws IOException
    {
        this.port = port;
        this.dc = dc;
        this.selector = SelectorProvider.provider().openSelector();
        selectionKey = dc.register(selector, OP_READ);
        broadcast = new InetSocketAddress("255.255.255.255", port);
        populateLocals(locals);
    }
    public static SimpleSync open(int port) throws IOException
    {
        DatagramChannel c = DatagramChannel.open();
        c.setOption(SO_BROADCAST, true);
        InetSocketAddress ba = new InetSocketAddress(port);
        c.bind(ba);
        c.setOption(IP_MULTICAST_LOOP, false);
        c.configureBlocking(false);
        return new SimpleSync(port, c);
    }

    public void sync() throws IOException
    {
        int countdown = 1000;
        int synced = 0;
        System.err.println("sync phase="+phase);
        send(synced);
        while (true)
        {
            int count = selector.select(100);
            if (count > 0)
            {
                try
                {
                    InetSocketAddress isa;
                    do
                    {
                        bb.clear();
                        isa = (InetSocketAddress) dc.receive(bb);
                        if (isa != null && !locals.contains(isa.getAddress()))
                        {
                            bb.flip();
                            int cnt = bb.remaining();
                            byte b = bb.get();
                            if (b == phase)
                            {
                                synced = cnt;
                                countdown -= Math.pow(10, synced);
                                send(synced);
                            }
                        }
                    } while (isa != null);
                }
                finally
                {
                    selector.selectedKeys().remove(selectionKey);
                }
            }
            else
            {
                send(synced);
            }
            if (countdown <= 0)
            {
                System.err.println("synced "+phase);
                phase++;
                return;
            }
            countdown--;
        }
    }
    private void send(int synced) throws IOException
    {
        //System.err.println("send="+phase);
        bb.clear();
        for (int ii=0;ii<=synced;ii++)
        {
            bb.put(phase);
        }
        bb.flip();
        dc.send(bb, broadcast);
    }
    @Override
    public void close() throws IOException
    {
        dc.close();
    }

    private void populateLocals(List<InetAddress> locals) throws SocketException
    {
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        while (nis.hasMoreElements())
        {
            NetworkInterface ni = nis.nextElement();
            Enumeration<InetAddress> ias = ni.getInetAddresses();
            while (ias.hasMoreElements())
            {
                locals.add(ias.nextElement());
            }
        }
    }
    
}
