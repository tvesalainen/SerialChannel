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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import static java.net.StandardSocketOptions.IP_MULTICAST_LOOP;
import static java.net.StandardSocketOptions.SO_BROADCAST;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import static java.nio.channels.SelectionKey.OP_READ;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;

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
    private SimpleSync(int port, DatagramChannel dc) throws IOException
    {
        this.port = port;
        this.dc = dc;
        this.selector = SelectorProvider.provider().openSelector();
        dc.register(selector, OP_READ);
    }
    public static SimpleSync open(int port) throws IOException
    {
        DatagramChannel c = DatagramChannel.open();
        c.setOption(IP_MULTICAST_LOOP, false);
        c.setOption(SO_BROADCAST, true);
        InetSocketAddress ba = new InetSocketAddress(port);
        c.bind(ba);
        c.configureBlocking(false);
        return new SimpleSync(port, c);
    }

    public void sync() throws IOException
    {
        InetSocketAddress ca = new InetSocketAddress("255.255.255.255", port);
        while (true)
        {
            bb.clear();
            bb.put(phase);
            bb.flip();
            dc.send(bb, ca);
            int count = selector.select(1000);
            if (count > 0)
            {
                bb.clear();
                SocketAddress sa = dc.receive(bb);
                bb.flip();
                byte b = bb.get();
                if (b == phase)
                {
                    return;
                }
                if (b > phase)
                {
                    throw new IllegalStateException("got "+b);
                }
            }
        }
    }
    @Override
    public void close() throws IOException
    {
        dc.close();
    }
    
}
