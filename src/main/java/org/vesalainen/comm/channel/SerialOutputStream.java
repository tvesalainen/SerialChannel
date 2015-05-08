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
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 *
 * @author tkv
 */
class SerialOutputStream extends OutputStream
{
    private SerialChannel channel;
    private ByteBuffer buffer;

    public SerialOutputStream(SerialChannel channel, int bufferSize)
    {
        this.channel = channel;
        buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    @Override
    public void flush() throws IOException
    {
        flushBuffer();
        channel.flush();
    }

    @Override
    public void write(int b) throws IOException
    {
        if (!buffer.hasRemaining())
        {
            flushBuffer();
        }
        buffer.put((byte) b);
    }

    public void flushBuffer() throws IOException
    {
        buffer.flip();
        while (buffer.hasRemaining())
        {
            channel.write(buffer);
        }
        buffer.clear();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        while (len > 0)
        {
            if (!buffer.hasRemaining())
            {
                flushBuffer();
            }
            int length = Math.min(len, buffer.remaining());
            buffer.put(b, off, length);
            len -= length;
            off += length;
        }
    }
    /**
     * Flushed the buffer. This method doesn't close the channel.
     * @throws IOException 
     */
    @Override
    public void close() throws IOException
    {
        flush();
        channel = null;
        buffer = null;
    }
    
}
