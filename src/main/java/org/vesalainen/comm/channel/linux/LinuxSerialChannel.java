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
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import java.util.List;
import java.util.Set;
import org.vesalainen.comm.channel.SerialChannel;
import org.vesalainen.comm.channel.SerialSelectionKey;
import org.vesalainen.loader.LibraryLoader;

/**
 *
 * @author tkv
 */
public class LinuxSerialChannel extends SerialChannel
{
    public static final int VERSION = 1;
    public static final int MaxSelectors = 64;
    private static long[] reads = new long[MaxSelectors];
    private static long[] writes = new long[MaxSelectors];

    private int min=1;
    private int time;
    
    static
    {                                       
        try
        {
            LibraryLoader.loadLibrary(LinuxSerialChannel.class, "SerialChannel");
        }
        catch (IOException | UnsatisfiedLinkError ex)
        {
            throw new UnsatisfiedLinkError("Can't load either x86_64 or arm6vl .so \n"+ex.getMessage());
        }
        staticInit();
    }

    public LinuxSerialChannel(String port)
    {
        this.port = port;
        int version = version();
        if (version != VERSION)
        {
            throw new UnsatisfiedLinkError("Loaded DLL version was"+version+" needed version "+VERSION);
        }
    }
    
    @Override
    protected native void doClearBuffers(long address);

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
    protected native int doRead(long handle, ByteBuffer dst) throws IOException;

    @Override
    protected native int doWrite(long handle, ByteBuffer src) throws IOException;

    public static native void setDebug(boolean on);

    public static native void doEnumPorts(List<String> list);

    @Override
    protected native void doClose(long handle) throws IOException;

    private static final byte[] errorReplacement = new byte[] {(byte)0xff, 0x00};

    public static byte[] errorReplacement()
    {
        return errorReplacement;
    }

    protected void checkVersion()
    {
        int version = version();
        if (version != VERSION)
        {
            throw new UnsatisfiedLinkError("Loaded DLL version was "+version+" needed version "+VERSION);
        }
    }

    private static native void staticInit();
    
    public static int doSelect(Set<SelectionKey> keys, Set<SelectionKey> selected, int timeout)
    {
        int updated = 0;
        int readIndex = 0;
        int writeIndex = 0;
        for (SelectionKey sk : keys)
        {
            LinuxSerialChannel channel = (LinuxSerialChannel) sk.channel();
            int interestOps = sk.interestOps();
            int mask = 0;
            if ((interestOps & OP_READ) != 0)
            {
                reads[readIndex++] = channel.address;
            }
            if ((interestOps & OP_WRITE) != 0)
            {
                writes[writeIndex++] = channel.address;
            }
        }
        int rc = LinuxSerialChannel.doSelect(readIndex, writeIndex, reads, writes, timeout);
        if (rc != 0)
        {
            readIndex = 0;
            writeIndex = 0;
            for (SelectionKey sk : keys)
            {
                int interestOps = sk.interestOps();
                int readyOps = 0;
                if ((interestOps & OP_READ) != 0)
                {
                    if (reads[readIndex++] != 0)
                    {
                        readyOps |= OP_READ;
                    }
                }
                if ((interestOps & OP_WRITE) != 0)
                {
                    if (writes[writeIndex++] != 0)
                    {
                        readyOps |= OP_WRITE;
                    }
                }
                if (readyOps != 0)
                {
                    SerialSelectionKey ssk = (SerialSelectionKey) sk;
                    if (selected.contains(sk))
                    {
                        if (ssk.readyOps() != readyOps)
                        {
                            updated++;
                            ssk.readyOps(readyOps);
                        }
                    }
                    else
                    {
                        updated++;
                        ssk.readyOps(readyOps);
                        selected.add(sk);
                    }
                }
            }
        }
        return updated;
    }

    private static native int doSelect(int readCount, int writeCount, long[] reads, long[] writes, int timeout);

    @Override
    protected void setTimeouts() throws IOException
    {
        if (address != -1)
        {
            if (block)
            {
                timeouts(address, min, time);
            }
            else
            {
                timeouts(address, 0, 10);
            }
        }
    }
    private native void timeouts(
            long handle,
            int min,
            int time
    ) throws IOException;
    
    @Override
    public int validOps()
    {
        return OP_READ | OP_WRITE;
    }

    public static void wakeupSelect(Set<SelectionKey> keys)
    {
        try
        {
            wakeupSelect();
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private static native void wakeupSelect() throws IOException;
    
}
