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
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import java.util.List;
import java.util.Set;
import org.vesalainen.comm.channel.SerialSelectionKey;
import org.vesalainen.loader.LibraryLoader;

/**
 *
 * @author tkv
 */
public class WinSerialChannel extends SerialChannel
{
    public static final int VERSION = 7;
    public static final int MAXDWORD = 0xffffffff;
    public static final int EV_RXCHAR = 0x0001;
    public static final int MaxSelect = 64;
    private static long[] handles = new long[MaxSelect];
    private static int[] masks = new int[MaxSelect];

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

    public WinSerialChannel(String port)
    {
        this.port = port;
    }

    @Override
    protected native void doConfigure(
            long handle,
            int baudRate, 
            int parity, 
            int dataBits, 
            int stopBits, 
            int flowControl,
            boolean replaceError
    ) throws IOException;

    @Override
    protected native int version();

    @Override
    protected native long doOpen(byte[] port);

    @Override
    protected native void doFlush(long handle) throws IOException;

    @Override
    protected native int doRead(long handle, ByteBuffer dst) throws IOException;

    @Override
    protected native int doWrite(long handle, ByteBuffer src) throws IOException;

    public static native void setDebug(boolean on);

    public static native void doEnumPorts(List<String> list);

    @Override
    protected native void doClose(long handle) throws IOException;

    protected void checkVersion()
    {
        int version = version();
        if (version != VERSION)
        {
            throw new UnsatisfiedLinkError("Loaded DLL version was "+version+" needed version "+VERSION);
        }
    }
    
    @Override
    protected void setTimeouts() throws IOException
    {
        timeouts(handle,
                readIntervalTimeout,
                readTotalTimeoutMultiplier,
                readTotalTimeoutConstant,
                writeTotalTimeoutMultiplier,
                writeTotalTimeoutConstant
        );
    }
    private native void timeouts(
            long handle,
            int readIntervalTimeout,
            int readTotalTimeoutMultiplier,
            int readTotalTimeoutConstant,
            int writeTotalTimeoutMultiplier,
            int writeTotalTimeoutConstant
    ) throws IOException;
    
    @Override
    public int validOps()
    {
        return OP_READ;
    }
    
    public static int doSelect(Set<SelectionKey> keys, Set<SelectionKey> selected, int timeout) throws IOException
    {
        int index = 0;
        for (SelectionKey sk : keys)
        {
            WinSerialChannel channel = (WinSerialChannel) sk.channel();
            handles[index] = channel.handle;
            int interestOps = sk.interestOps();
            int mask = 0;
            if ((interestOps & OP_READ) != 0)
            {
                mask |= EV_RXCHAR;
            }
            masks[index] = mask;
            index++;
        }
        int rc = WinSerialChannel.doSelect(index, handles, masks, timeout);
        if (rc != 0)
        {
            index = 0;
            for (SelectionKey sk : keys)
            {
                int readyOps = 0;
                if ((masks[index] & EV_RXCHAR) != 0)
                {
                    readyOps |= OP_READ;
                }
                if (readyOps != 0)
                {
                    SerialSelectionKey ssk = (SerialSelectionKey) sk;
                    ssk.readyOps(readyOps);
                    selected.add(sk);
                }
                index++;
            }
        }
        return rc;
    }
    private native void setEventMask(long handle, int mask) throws IOException;

    private native int waitEvent(long handle, int mask) throws IOException;

    private static native int doSelect(int len, long[] handles, int[] masks, int timeout) throws IOException;

    private static final byte[] errorReplacement = new byte[] {(byte)0xff};
    @Override
    public byte[] getErrorReplacement()
    {
        return errorReplacement;
    }

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
                    waitEvent(EV_RXCHAR);
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

    @Override
    public String toString()
    {
        return "WinSerialChannel{" + port + '}';
    }

    protected void setEventMask(int mask) throws IOException
    {
        setEventMask(handle, mask);
    }

    protected int waitEvent(int mask) throws IOException
    {
        return waitEvent(handle, mask);
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

    public static void wakeupSelect(Set<SelectionKey> keys)
    {
        try
        {
            for (SelectionKey sk : keys)
            {
                WinSerialChannel channel = (WinSerialChannel) sk.channel();
                channel.setEventMask(0);
            }
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }
    
}
