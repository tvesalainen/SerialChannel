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
import java.util.AbstractSet;
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
    public SerialSelector()
    {
        super(SerialSelectorProvider.provider());
    }

    @Override
    protected void implCloseSelector() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att)
    {
        SelectionKey sk = new SerialSelectionKey(ch, this, ops);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int select(long timeout) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int select() throws IOException
    {
        return SerialChannel.select(keys, selected);
    }

    @Override
    public Selector wakeup()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
