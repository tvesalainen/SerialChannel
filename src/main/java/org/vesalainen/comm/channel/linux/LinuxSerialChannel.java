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
package org.vesalainen.comm.channel.linux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.vesalainen.comm.channel.CommError;
import org.vesalainen.comm.channel.CommStat;
import org.vesalainen.comm.channel.CommStatus;
import org.vesalainen.comm.channel.SerialChannel;

/**
 *
 * @author tkv
 */
public class LinuxSerialChannel extends SerialChannel
{
    public static final int VERSION = 1;
    private long handle = -1;
    private int min;
    private int time;
    
    public static int doSelect(Set<SelectionKey> keys, Set<SelectionKey> selected, int timeout)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static List<String> getAllPorts()
    {
        List<String> list = new ArrayList<>();
        doEnumPorts(list);
        return list;
    }

    private static native void doEnumPorts(List<String> list);

    @Override
    protected void connect() throws IOException
    {
        handle = initialize(
                port.getBytes(), 
                getSpeed(speed), 
                parity.ordinal(), 
                dataBits.ordinal(), 
                stopBits.ordinal(), 
                flowControl.ordinal()
        );
        setTimeouts();
    }

        private void setTimeouts() throws IOException
    {
        if (handle != -1)
        {
            if (block)
            {
                timeouts(handle, min, time);
            }
            else
            {
                timeouts(handle, 0, 0);
            }
        }
    }
    private native long initialize(
            byte[] port, 
            int baudRate, 
            int parity, 
            int dataBits, 
            int stopBits, 
            int flowControl
    ) throws IOException;

    private native void timeouts(
            long handle,
            int min,
            int time
    ) throws IOException;
    
    @Override
    public void flush() throws IOException
    {
        doFlush(handle);
    }

    private native void doFlush(long handle) throws IOException;

    @Override
    public boolean isConnected()
    {
        return connected(handle);
    }

    private native boolean connected(long handle);
    
    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        if (handle != -1)
        {
            int count = 0;
            try
            {
                begin();
                return doRead(handle, dst);
            }
            finally
            {
                end(count > 0);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    private native int doRead(long handle, ByteBuffer dst) throws IOException;

    @Override
    public int write(ByteBuffer src) throws IOException
    {
        if (handle != -1)
        {
            int count = 0;
            try
            {
                begin();
                count = doWrite(handle, src);
                return count;
            }
            finally
            {
                end(count > 0);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    private native int doWrite(long handle, ByteBuffer src) throws IOException;

    @Override
    public CommStatus getCommStatus() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void doClose() throws IOException
    {
        doClose(handle);
        handle = -1;
    }

    private native void doClose(long handle) throws IOException;

    @Override
    protected void setEventMask(int mask) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected int waitEvent(int mask) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected CommError getError(CommStat stat) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int validOps()
    {
        return OP_READ | OP_WRITE;
    }

    @Override
    public void wakeupSelect() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
