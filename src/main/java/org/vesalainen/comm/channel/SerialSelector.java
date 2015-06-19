
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
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.vesalainen.util.concurrent.ConcurrentArraySet;

/**
 *
 * @author tkv
 */
public class SerialSelector extends AbstractSelector
{
    private final Set<SelectionKey> keys = new ConcurrentArraySet<>();
    private final Set<SelectionKey> unmodifiableKeys = Collections.unmodifiableSet(keys);
    private final Set<SelectionKey> selected = new ConcurrentArraySet<>();
    private final Set<Thread> threads = new ConcurrentArraySet<>();
    private boolean wakeupPending;
    
    SerialSelector()
    {
        super(SerialSelectorProvider.provider());
    }

    public static SerialSelector open() throws IOException 
    {
        return SerialSelectorProvider.provider().openSelector();
    }
    
    @Override
    protected void implCloseSelector() throws IOException
    {
        synchronized(threads)
        {
            if (!threads.isEmpty())
            {
                wakeup();
            }
        }
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att)
    {
        synchronized(keys)
        {
            if (ch.isBlocking())
            {
                throw new IllegalArgumentException("blocking not allowed");
            }
            if ((ops & ~ch.validOps()) != 0)
            {
                throw new IllegalArgumentException("ops is not supported");
            }
            SelectionKey sk = new SerialSelectionKey(ch, this, ops, att);
            keys.add(sk);
            return sk;
        }
    }

    @Override
    public Set<SelectionKey> keys()
    {
        return unmodifiableKeys;
    }

    @Override
    public Set<SelectionKey> selectedKeys()
    {
        return selected;
    }

    @Override
    public int selectNow() throws IOException
    {
        return select(0);
    }

    @Override
    public synchronized int select(long timeout) throws IOException
    {
        if (wakeupPending)
        {
            wakeupPending = false;
            return 0;
        }
        handleCancelled();
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
                handleCancelled();
            }
        }
        return 0;
    }

    private void handleCancelled()
    {
        Set<SelectionKey> cancelledKeys = cancelledKeys();
        synchronized(cancelledKeys)
        {
            Iterator<SelectionKey> iterator = cancelledKeys.iterator();
            while (iterator.hasNext())
            {
                SelectionKey sk = iterator.next();
                deregister((AbstractSelectionKey)sk);
                keys.remove(sk);
                iterator.remove();
            }
        }
    }
    @Override
    public int select() throws IOException
    {
        return select(-1);
    }

    @Override
    public Selector wakeup()
    {
        if (!threads.isEmpty())
        {
            SerialChannel.wakeupSelect(keys);
        }
        else
        {
            wakeupPending = true;
        }
        return this;
    }
}
