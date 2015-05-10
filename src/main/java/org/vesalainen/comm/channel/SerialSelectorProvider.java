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
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

/**
 *
 * @author tkv
 */
public class SerialSelectorProvider extends SelectorProvider
{
    private static SerialSelectorProvider provider = new SerialSelectorProvider();
    
    public static SerialSelectorProvider provider()
    {
        return provider;
    }

    @Override
    public DatagramChannel openDatagramChannel() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Pipe openPipe() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AbstractSelector openSelector() throws IOException
    {
        return new SerialSelector();
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SocketChannel openSocketChannel() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
