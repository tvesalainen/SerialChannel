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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tkv
 */
public class SerialSelector extends AbstractSelector
{
    private final Set<SelectionKey> keys = new HashSet<>();
    private final Set<SelectionKey> selected = new HashSet<>();
    private final Set<Thread> threads = Collections.synchronizedSet(new HashSet<Thread>());
    
    public SerialSelector()
    {
        super(SerialSelectorProvider.provider());
    }

    @Override
    protected void implCloseSelector() throws IOException
    {
        synchronized(threads)
        {
            for (Thread t : threads)
            {
                t.interrupt();
            }
        }
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att)
    {
        if ((ops & ~ch.validOps()) != 0)
        {
            throw new IllegalArgumentException("ops is not supported");
        }
        SelectionKey sk = new SerialSelectionKey(ch, this, ops, att);
        keys.add(sk);
        return sk;
    }

    @Override
    public Set<SelectionKey> keys()
    {
        return keys;    // TODO immutable
    }

    @Override
    public Set<SelectionKey> selectedKeys()
    {
        return selected;    // TODO immutable
    }

    @Override
    public int selectNow() throws IOException
    {
        return select(0);
    }

    @Override
    public int select(long timeout) throws IOException
    {
        Set<SelectionKey> cancelledKeys = cancelledKeys();
        synchronized(cancelledKeys)
        {
            keys.removeAll(cancelledKeys);
        }
        if (!keys.isEmpty())
        {
            begin();
            Thread currentThread = Thread.currentThread();
            threads.add(currentThread);
            try
            {
                return SerialChannel.select(keys, selected, (int)timeout);
            }
            finally
            {
                threads.remove(currentThread);
                end();
            }
        }
        return 0;
    }

    @Override
    public int select() throws IOException
    {
        return select(-1);
    }

    @Override
    public Selector wakeup()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
