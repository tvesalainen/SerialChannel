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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import java.util.List;
import java.util.Set;
import org.vesalainen.loader.LibraryLoader;

/**
 *
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class LinuxSerialChannel extends SerialChannel
{
    public static final int VERSION = 3;
    /**
     * The maximum number of buffers in Gathering or Scattering  operations.
     */
    public static final int MaxBuffers = 16;
    private static LongBuffer reads = ByteBuffer.allocateDirect(8*MaxSelectors)
            .order(ByteOrder.nativeOrder())
            .asLongBuffer();
    private static LongBuffer writes = ByteBuffer.allocateDirect(8*MaxSelectors)
            .order(ByteOrder.nativeOrder())
            .asLongBuffer();
    private ByteBuffer[] readBuffer = new ByteBuffer[1];
    private ByteBuffer[] writeBuffer = new ByteBuffer[1];
    
    /**
     *        
     * MIN == 0, TIME == 0 (polling read)
              If data is available, read(2) returns immediately, with the lesser of the num‐
              ber of bytes available, or the number of  bytes  requested.   If  no  data  is
              available, read(2) returns 0.

       MIN > 0, TIME == 0 (blocking read)
              read(2)  blocks until MIN bytes are available, and returns up to the number of
              bytes requested.

       MIN == 0, TIME > 0 (read with timeout)
              TIME specifies the limit for a timer in tenths of  a  second.   The  timer  is
              started when read(2) is called.  read(2) returns either when at least one byte
              of data is available, or when the timer expires.  If the timer expires without
              any input becoming available, read(2) returns 0.  If data is already available
              at the time of the call to read(2), the call behaves as though  the  data  was
              received immediately after the call.

       MIN > 0, TIME > 0 (read with interbyte timeout)
              TIME  specifies  the limit for a timer in tenths of a second.  Once an initial
              byte of input becomes available, the timer is  restarted  after  each  further
              byte  is  received.   read(2)  returns when any of the following conditions is
              met:

              *  MIN bytes have been received.

              *  The interbyte timer expires.

              *  The number of bytes requested by read(2) has been  received.   (POSIX  does
                 not  specify  this termination condition, and on some other implementations
                 read(2) does not return in this case.)

              Because the timer is started only after the initial byte becomes available, at
              least  one byte will be read.  If data is already available at the time of the
              call to read(2), the call behaves as though the data was received  immediately
              after the call.

     */
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

    LinuxSerialChannel(String port)
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
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
    {
        if (address != -1)
        {
            long count = 0;
            readLock.lock();
            try
            {
                begin();
                count = doRead(address, dsts, offset, length);
                return count;
            }
            finally
            {
                readLock.unlock();
                end(count > 0);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        readLock.lock();
        readBuffer[0] = dst;
        try
        {
            return (int) read(readBuffer, 0, 1);
        }
        finally
        {
            readLock.unlock();
        }
    }

    private native int doRead(long handle, ByteBuffer[] dsts, int offset, int length) throws IOException;

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
    {
        if (address != -1)
        {
            writeLock.lock();
            long count = 0;
            try
            {
                begin();
                count = doWrite(address, srcs, offset, length);
                return count;
            }
            finally
            {
                writeLock.unlock();
                end(count > 0);
            }
        }
        else
        {
            throw new ClosedChannelException();
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException
    {
        writeLock.lock();
        try
        {
            writeBuffer[0] = src;
            return (int) write(writeBuffer, 0, 1);
        }
        finally
        {
            writeLock.unlock();
        }
    }

    private native int doWrite(long handle, ByteBuffer[] srcs, int offset, int length) throws IOException;

    public static native void setDebug(boolean on);

    public static native void doEnumPorts(List<String> list);

    @Override
    protected native void doClose(long handle) throws IOException;

    @Override
    protected native void free(long handle);

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
    
    static int doSelect(Set<SelectionKey> keys, Set<SelectionKey> selected, int timeout)
    {
        int updated = 0;
        int readIndex = 0;
        int writeIndex = 0;
        for (SelectionKey sk : keys)
        {
            LinuxSerialChannel channel = (LinuxSerialChannel) sk.channel();
            assert channel.address != -1;
            int interestOps = sk.interestOps();
            if ((interestOps & OP_READ) != 0)
            {
                reads.put(readIndex++, channel.address);
            }
            if ((interestOps & OP_WRITE) != 0)
            {
                writes.put(writeIndex++, channel.address);
            }
        }
        log.finest("select(%d, %d, %d)", readIndex, writeIndex, timeout);
        int rc = doSelect(readIndex, writeIndex, reads, writes, timeout);
        log.finest("rc=%d", rc);
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
                    if (reads.get(readIndex++) != 0)
                    {
                        readyOps |= OP_READ;
                    }
                }
                if ((interestOps & OP_WRITE) != 0)
                {
                    if (writes.get(writeIndex++) != 0)
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

    private static native int doSelect(int readCount, int writeCount, LongBuffer reads, LongBuffer writes, int timeout);

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

    static void wakeupSelect(Set<SelectionKey> keys)
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

    @Override
    public String toString()
    {
        return "LinuxSerialChannel{" + port+": "+configuration+"}";
    }
    
}
