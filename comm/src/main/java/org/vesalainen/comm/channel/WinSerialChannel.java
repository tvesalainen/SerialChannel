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
package org.vesalainen.comm.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import java.util.List;
import java.util.Set;
import org.vesalainen.loader.LibraryLoader;

/**
 * A Windows implementation of SerialChannel
 *
 * <p>
 * Implementation notes:
 * <p>
 * Scattering/Gathering IO is implemented in java.
 * <p>
 * Select implementation uses CancelIo function which cancels all io for the
 * channel. Currently this will cause problem if one thread calls read/write,
 * while another thread calls select. Since select is usually used in one thread
 * applications, it is not problem at all.
 * <p>
 * If this class later implements AsynchronousChannel, the CancelIo has to be
 * implemented in another way!
 *
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class WinSerialChannel extends SerialChannel
{

    public static final int VERSION = 9;
    public static final int MAXDWORD = 0xffffffff;
    public static final int EV_RXCHAR = 0x0001;
    public static final int MaxSelect = 64;
    private static LongBuffer reads = ByteBuffer.allocateDirect(8 * MaxSelectors)
            .order(ByteOrder.nativeOrder())
            .asLongBuffer();
    /**
     * The maximum time allowed to elapse before the arrival of the next byte on
     * the communications line, in milliseconds. If the interval between the
     * arrival of any two bytes exceeds this amount, the ReadFile operation is
     * completed and any buffered data is returned. A value of zero indicates
     * that interval time-outs are not used. A value of MAXDWORD, combined with
     * zero values for both the ReadTotalTimeoutConstant and
     * ReadTotalTimeoutMultiplier members, specifies that the read operation is
     * to return immediately with the bytes that have already been received,
     * even if no bytes have been received.
     */
    private int readIntervalTimeout = MAXDWORD;
    /**
     * The multiplier used to calculate the total time-out period for read
     * operations, in milliseconds. For each read operation, this value is
     * multiplied by the requested number of bytes to be read.
     */
    private int readTotalTimeoutMultiplier = MAXDWORD;
    /**
     * A constant used to calculate the total time-out period for read
     * operations, in milliseconds. For each read operation, this value is added
     * to the product of the ReadTotalTimeoutMultiplier member and the requested
     * number of bytes. A value of zero for both the ReadTotalTimeoutMultiplier
     * and ReadTotalTimeoutConstant members indicates that total time-outs are
     * not used for read operations.
     */
    private int readTotalTimeoutConstant = 100;
    /**
     * The multiplier used to calculate the total time-out period for write
     * operations, in milliseconds. For each write operation, this value is
     * multiplied by the number of bytes to be written.
     */
    private int writeTotalTimeoutMultiplier;
    /**
     * A constant used to calculate the total time-out period for write
     * operations, in milliseconds. For each write operation, this value is
     * added to the product of the WriteTotalTimeoutMultiplier member and the
     * number of bytes to be written. A value of zero for both the
     * WriteTotalTimeoutMultiplier and WriteTotalTimeoutConstant members
     * indicates that total time-outs are not used for write operations.
     */
    private int writeTotalTimeoutConstant;

    static
    {
        try
        {
            LibraryLoader.loadLibrary(WinSerialChannel.class, "SerialChannel");
        }
        catch (IOException | UnsatisfiedLinkError ex)
        {
            throw new UnsatisfiedLinkError("Can't load either 32 or 64 .dll \n" + ex.getMessage());
        }
        staticInit();
    }

    WinSerialChannel(String port)
    {
        this.port = port;
    }

    @Override
    protected native void doClearBuffers(long address);

    private static native void staticInit();

    /**
     * Change channel configuration
     *
     * @param config
     * @throws IOException
     */
    @Override
    public void configure(Configuration config) throws IOException
    {
        this.configuration = config;
        doConfigure(address,
                getSpeed(configuration.speed),
                configuration.parity.ordinal(),
                configuration.dataBits.ordinal(),
                configuration.stopBits.ordinal(),
                configuration.flowControl.ordinal(),
                configuration.replaceError
        );
    }

    private native void doConfigure(
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

    private native int doRead(long handle, ByteBuffer dst) throws IOException;

    /**
     * Writes data at buffers position and then increments the position.
     * @param src
     * @return Returns number of characters written.
     * @throws IOException 
     */
    @Override
    public int write(ByteBuffer src) throws IOException
    {
        if (address != -1)
        {
            int count = 0;
            writeLock.lock();
            try
            {
                begin();
                count = doWrite(address, src);
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

    private native int doWrite(long handle, ByteBuffer src) throws IOException;

    public static native void setDebug(boolean on);

    public static native void doEnumPorts(List<String> list);

    @Override
    protected native void doClose(long handle) throws IOException;

    @Override
    protected native void free(long handle);

    protected void checkVersion()
    {
        int version = version();
        if (version != VERSION)
        {
            throw new UnsatisfiedLinkError("Loaded DLL version was " + version + " needed version " + VERSION);
        }
    }

    @Override
    protected void doBlocking() throws IOException
    {
        timeouts(address,
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

    static int doSelect(Set<SelectionKey> keys, Set<SelectionKey> selected, int timeout) throws IOException
    {
        int updated = 0;
        int readIndex = 0;
        synchronized (keys)
        {
            for (SelectionKey sk : keys)
            {
                WinSerialChannel channel = (WinSerialChannel) sk.channel();
                int interestOps = sk.interestOps();
                if ((interestOps & OP_READ) != 0)
                {
                    reads.put(readIndex++, channel.address);
                }
            }
        }
        int rc = WinSerialChannel.doSelect(readIndex, reads, timeout);
        if (rc != 0)
        {
            readIndex = 0;
            synchronized (keys)
            {
                for (SelectionKey sk : keys)
                {
                    int readyOps = 0;
                    if (reads.get(readIndex++) == 0)
                    {
                        readyOps |= OP_READ;
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
                            selected.add(ssk);
                        }
                    }
                }
            }
        }
        return updated;
    }

    private native void setEventMask(long handle, int mask) throws IOException;

    private native int waitEvent(long handle, int mask) throws IOException;

    private static native int doSelect(int len, LongBuffer handles, int timeout) throws IOException;

    private static final byte[] errorReplacement = new byte[]
    {
        (byte) 0xff, (byte) 0xff
    };

    public static byte[] errorReplacement()
    {
        return errorReplacement;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        if (address != -1)
        {
            int count = 0;
            try
            {
                begin();
                count = doRead(address, dst);
                while (block && count == 0)
                {
                    waitEvent(EV_RXCHAR);
                    count = doRead(address, dst);
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
        setEventMask(address, mask);
    }

    protected int waitEvent(int mask) throws IOException
    {
        return waitEvent(address, mask);
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

    static void wakeupSelect(Set<SelectionKey> keys)
    {
        try
        {
            synchronized (keys)
            {
                for (SelectionKey sk : keys)
                {
                    WinSerialChannel channel = (WinSerialChannel) sk.channel();
                    channel.setEventMask(0);
                }
            }
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }

}
