/*
 * Copyright (C) 2015 Timo Vesalainen <timo.vesalainen@iki.fi>
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

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

/**
 *
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class SerialSelectionKey extends AbstractSelectionKey
{
    private final SelectableChannel channel;
    private final Selector selector;
    private int interestOps;
    private int readyOps;

    public SerialSelectionKey(SelectableChannel channel, Selector selector, int interestOps, Object attachment)
    {
        this.channel = channel;
        this.selector = selector;
        this.interestOps = interestOps;
        attach(attachment);
    }
    
    @Override
    public SelectableChannel channel()
    {
        return channel;
    }

    @Override
    public Selector selector()
    {
        return selector;
    }

    @Override
    public int interestOps()
    {
        return interestOps;
    }

    @Override
    public SelectionKey interestOps(int ops)
    {
        this.interestOps = ops;
        return this;
    }

    @Override
    public int readyOps()
    {
        return readyOps;
    }
    
    public void readyOps(int ops)
    {
        this.readyOps = ops;
    }

    @Override
    public String toString()
    {
        return "SerialSelectionKey{" + "channel=" + channel + ", readyOps=" + readyOps + '}';
    }
    
}
