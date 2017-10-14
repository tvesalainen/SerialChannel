/*
 * Copyright (C) 2013 tkv
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
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 *
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class SerialInputStream extends InputStream
{
    private SerialChannel channel;
    private ByteBuffer buffer;
    private boolean online;

    public SerialInputStream(SerialChannel channel, int bufferSize)
    {
        this.channel = channel;
        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.flip();
    }

    @Override
    public int read() throws IOException
    {
        /*
        if (!online)
        {
        channel.waitOnline();
        online = true;
        }
         */
        if (!buffer.hasRemaining())
        {
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
        }
        try
        {
            return buffer.get() & 0xff;
        }
        catch (BufferUnderflowException ex)
        {
            return -1;
        }
    }

    @Override
    public int available() throws IOException
    {
        return buffer.remaining();
    }

    @Override
    public synchronized void mark(int readlimit)
    {
        if (buffer.remaining() < readlimit)
        {
            throw new IllegalArgumentException("Couldn't set mark for " + readlimit + " bytes");
        }
        buffer.mark();
    }

    @Override
    public boolean markSupported()
    {
        return true;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (!buffer.hasRemaining())
        {
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
        }
        int length = Math.min(len, buffer.remaining());
        buffer.get(b, off, length);
        return length;
    }

    @Override
    public synchronized void reset() throws IOException
    {
        buffer.reset();
    }
    /**
     * This method doesn't close the channel.
     * @throws IOException 
     */
    @Override
    public void close() throws IOException
    {
        channel = null;
        buffer = null;
    }
    
    @Override
    public String toString()
    {
        return "SerialInputStream{" + "channel=" + channel + "buffer=" + buffer + '}';
    }
    
}
