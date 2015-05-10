/*
 * Copyright (C) 2011 Timo Vesalainen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.vesalainen.comm.channel.winx;

import org.vesalainen.comm.channel.SerialChannel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.vesalainen.comm.channel.CommError;
import org.vesalainen.comm.channel.CommStat;
import org.vesalainen.comm.channel.CommStatus;
import org.vesalainen.comm.channel.SerialSelectionKey;
import static org.vesalainen.comm.channel.winx.WinCommEvent.CHAR;
import static org.vesalainen.comm.channel.winx.WinCommEvent.EMPTY;
import org.vesalainen.loader.LibraryLoader;

/**
 *
 * @author tkv
 */
public class WinSerialChannel extends SerialChannel
{
    public static final int VERSION = 7;
    public static final int MAXDWORD = 0xffffffff;

    private long handle = -1;
    private int readIntervalTimeout = MAXDWORD;
    private int readTotalTimeoutMultiplier = MAXDWORD;
    private int readTotalTimeoutConstant = 100;
    private int writeTotalTimeoutMultiplier;
    private int writeTotalTimeoutConstant;

    static
    {
        try
        {
            LibraryLoader.loadLibrary(WinSerialChannel.class, "SerialChannel");
        }
        catch (IOException | UnsatisfiedLinkError ex)
        {
            throw new UnsatisfiedLinkError("Can't load either 32 or 64 .dll \n"+ex.getMessage());
        }
    }

    public WinSerialChannel(String port, Speed speed, Parity parity, StopBits stopBits, DataBits dataBits, FlowControl flowControl)
    {
        int version = version();
        if (version != VERSION)
        {
            throw new UnsatisfiedLinkError("Loaded DLL version was"+version+" needed version "+VERSION);
        }
        this.port = port;
        this.speed = speed;
        this.parity = parity;
        this.stopBits = stopBits;
        this.dataBits = dataBits;
        this.flowControl = flowControl;
    }

    @Override
    protected void connect() throws IOException
    {
        handle = initialize(
                port.getBytes(), 
                SPEED[speed.ordinal()], 
                parity.ordinal(), 
                dataBits.ordinal(), 
                stopBits.ordinal(), 
                flowControl.ordinal(),
                readIntervalTimeout,
                readTotalTimeoutMultiplier,
                readTotalTimeoutConstant,
                writeTotalTimeoutMultiplier,
                writeTotalTimeoutConstant
        );
    }
    
    @Override
    public int validOps()
    {
        return OP_READ;
    }
    
    public static int doSelect(Set<SelectionKey> keys, Set<SelectionKey> selected) throws IOException
    {
        selected.clear();
        long[] handles = new long[keys.size()];
        int[] masks = new int[handles.length];
        int index = 0;
        for (SelectionKey sk : keys)
        {
            WinSerialChannel channel = (WinSerialChannel) sk.channel();
            handles[index] = channel.handle;
            int interestOps = sk.interestOps();
            int mask = 0;
            if ((interestOps & OP_READ) != 0)
            {
                mask |= CHAR;
            }
            masks[index] = mask;
            index++;
        }
        int rc = WinSerialChannel.doSelect(handles, masks);
        if (rc != 0)
        {
            index = 0;
            for (SelectionKey sk : keys)
            {
                if (masks[index] != 0)
                {
                    SerialSelectionKey ssk = (SerialSelectionKey) sk;
                    ssk.readyOps(OP_READ);
                    selected.add(sk);
                }
                index++;
            }
        }
        return rc;
    }
    private native long initialize(
            byte[] port, 
            int baudRate, 
            int parity, 
            int dataBits, 
            int stopBits, 
            int flowControl,
            int readIntervalTimeout,
            int readTotalTimeoutMultiplier,
            int readTotalTimeoutConstant,
            int writeTotalTimeoutMultiplier,
            int writeTotalTimeoutConstant
    ) throws IOException;

    private native int version();

    private native void setEventMask(long handle, int mask) throws IOException;

    private native int waitEvent(long handle, int mask) throws IOException;

    private static native int doSelect(long[] handles, int[] masks) throws IOException;

    @Override
    protected CommError getError(CommStat stat) throws IOException
    {
        int err = doGetError(handle, (WinCommStat)stat);
        return new WinCommError(err);
    }

    private native int doGetError(long handle, WinCommStat stat) throws IOException;

    public static void debug(boolean on)
    {
        setDebug(on);
    }

    private static native void setDebug(boolean on);

    @Override
    public boolean isConnected()
    {
        return connected(handle);
    }

    private native boolean connected(long handle);
    
    @Override
    public void flush() throws IOException
    {
        doFlush(handle);
    }

    public static List<String> getAllPorts()
    {
        List<String> list = new ArrayList<>();
        doEnumPorts(list);
        return list;
    }

    private static native void doEnumPorts(List<String> list);

    protected int commStatus() throws IOException
    {
        return commStatus(handle);
    }

    @Override
    public CommStatus getCommStatus() throws IOException
    {
        return new WinCommStatus(this);
    }

    protected native int commStatus(long handle) throws IOException;

    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        if (handle != -1)
        {
            int count = 0;
            try
            {
                begin();
                count = doRead(handle, dst);
                while (block && count == 0)
                {
                    waitEvent(0x0001);
                    count = doRead(handle, dst);
                }
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
    private native int doRead(long handle, ByteBuffer dst) throws IOException;

    private native void doClose(long handle) throws IOException;

    private native void doFlush(long handle) throws IOException;

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
    public String getPort()
    {
        return getPort();
    }

    @Override
    public String toString()
    {
        return "WinSerialChannel{" + "handle=" + handle + "port=" + port + '}';
    }

    @Override
    protected void setEventMask(int mask) throws IOException
    {
        setEventMask(handle, mask);
    }

    @Override
    protected int waitEvent(int mask) throws IOException
    {
        return waitEvent(handle, mask);
    }

    @Override
    protected void doClose() throws IOException
    {
        doClose(handle);
        handle = -1;
    }

    public int getReadIntervalTimeout()
    {
        return readIntervalTimeout;
    }

    public WinSerialChannel setReadIntervalTimeout(int readIntervalTimeout)
    {
        this.readIntervalTimeout = readIntervalTimeout;
        return this;
    }

    public int getReadTotalTimeoutMultiplier()
    {
        return readTotalTimeoutMultiplier;
    }

    public WinSerialChannel setReadTotalTimeoutMultiplier(int readTotalTimeoutMultiplier)
    {
        this.readTotalTimeoutMultiplier = readTotalTimeoutMultiplier;
        return this;
    }

    public int getReadTotalTimeoutConstant()
    {
        return readTotalTimeoutConstant;
    }

    public WinSerialChannel setReadTotalTimeoutConstant(int readTotalTimeoutConstant)
    {
        this.readTotalTimeoutConstant = readTotalTimeoutConstant;
        return this;
    }

    public int getWriteTotalTimeoutMultiplier()
    {
        return writeTotalTimeoutMultiplier;
    }

    public WinSerialChannel setWriteTotalTimeoutMultiplier(int writeTotalTimeoutMultiplier)
    {
        this.writeTotalTimeoutMultiplier = writeTotalTimeoutMultiplier;
        return this;
    }

    public int getWriteTotalTimeoutConstant()
    {
        return writeTotalTimeoutConstant;
    }

    public WinSerialChannel setWriteTotalTimeoutConstant(int writeTotalTimeoutConstant)
    {
        this.writeTotalTimeoutConstant = writeTotalTimeoutConstant;
        return this;
    }
    
}
