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
import java.util.ArrayList;
import java.util.List;
import org.vesalainen.comm.channel.CommError;
import org.vesalainen.comm.channel.CommStat;
import org.vesalainen.comm.channel.CommStatus;
import org.vesalainen.loader.LibraryLoader;

/**
 *
 * @author tkv
 */
public class WinSerialChannel extends SerialChannel
{
    public static final int VERSION = 4;
    private static final int InitialSleep = 10;

    private long handle = -1;
    private int sleep = InitialSleep;

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

    public WinSerialChannel(String port, Speed speed)
    {
        int version = version();
        if (version != VERSION)
        {
            throw new UnsatisfiedLinkError("Loaded DLL version was"+version+" needed version "+VERSION);
        }
        this.port = port;
        this.speed = speed;
    }

    @Override
    public void connect() throws IOException
    {
        handle = initialize(port.getBytes(), SPEED[speed.ordinal()], parity.ordinal(), dataBits.ordinal(), stopBits.ordinal(), flowControl.ordinal());
    }

    private native long initialize(byte[] port, int baudRate, int parity, int dataBits, int stopBits, int flowControl) throws IOException;

    private native int version();

    private native void setEventMask(long handle, int mask) throws IOException;

    private native int waitEvent(long handle) throws IOException;

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

    @Override
    public void waitOnline()
    {
        doWaitOnline(handle);
    }

    private native void doWaitOnline(long handle);

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
                // for some reason Windows driver is not always in blocking mode?
                sleep = sleep > InitialSleep ? sleep - 1 : InitialSleep;
                while (count == 0)
                {
                    Thread.sleep(sleep);
                    count = doRead(handle, dst);
                    sleep++;
                }
                System.err.println(sleep);
                return count;
            }
            catch (InterruptedException ex)
            {
                throw new IOException(ex);
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
    protected void finalize() throws Throwable
    {
        close();
        super.finalize();
    }

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
    protected int waitEvent() throws IOException
    {
        return waitEvent(handle);
    }

    @Override
    protected void doClose() throws IOException
    {
        doClose(handle);
        handle = -1;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            WinSerialChannel sc = new WinSerialChannel("COM8", WinSerialChannel.Speed.CBR_115200);
            ByteBuffer bb = ByteBuffer.allocateDirect(10);
            sc.connect();
            sc.read(bb);
            System.err.println(bb);
            sc.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
