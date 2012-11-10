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

import org.vesalainen.comm.channel.CommEvent;
import org.vesalainen.comm.channel.CommEventObserver;
import org.vesalainen.comm.channel.SerialChannel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tkv
 */
public class WinSerialChannel extends SerialChannel implements Runnable
{
    public static final int VERSION = 4;

    private long handle = -1;
    private Map<CommEventObserver,Set<CommEvent.Type>> eventObserverMap;
    private Set<CommEvent.Type> observedEvents;
    private Thread eventObserverThread;

    static
    {
        try
        {
            System.loadLibrary("SerialChannel64");
        }
        catch (UnsatisfiedLinkError ex)
        {
            try
            {
                System.loadLibrary("SerialChannel32");
            }
            catch (UnsatisfiedLinkError ex2)
            {
                throw new UnsatisfiedLinkError("Can't load either 32 or 64 .dll \n"+ex2.getMessage());
            }
        }
    }

    public WinSerialChannel(String port, Speed speed)
    {
        this(port, speed, Parity.NONE, DataBits.DATABITS_8, StopBits.STOPBITS_10, FlowControl.NONE);
    }

    public WinSerialChannel(String port, int speed)
    {
        this(port, Speed.valueOf("CBR_"+speed), Parity.NONE, DataBits.DATABITS_8, StopBits.STOPBITS_10, FlowControl.NONE);
    }

    public WinSerialChannel(String port, Speed speed, Parity parity, DataBits dataBits, StopBits stopBits, FlowControl flowControl)
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
    public void connect() throws IOException
    {
        handle = initialize(port.getBytes(), SPEED[speed.ordinal()], parity.ordinal(), dataBits.ordinal(), stopBits.ordinal(), flowControl.ordinal());
    }

    private native long initialize(byte[] port, int baudRate, int parity, int dataBits, int stopBits, int flowControl) throws IOException;

    private native int version();

    @Override
    public void addEventObserver(CommEventObserver observer, CommEvent.Type... types) throws IOException
    {
        if (eventObserverMap == null)
        {
            eventObserverMap = new HashMap<CommEventObserver,Set<CommEvent.Type>>();
            observedEvents = EnumSet.noneOf(CommEvent.Type.class);
        }
        Set<CommEvent.Type> set = EnumSet.copyOf(Arrays.asList(types));
        eventObserverMap.put(observer, set);
        observedEvents.addAll(set);
        setEventMask(handle, WinCommEvent.createEventMask(observedEvents));
        if (eventObserverThread == null && !observedEvents.isEmpty())
        {
            eventObserverThread = new Thread(this);
            eventObserverThread.start();
        }
    }

    private native void setEventMask(long handle, int mask) throws IOException;

    @Override
    public boolean removeEventObserver(CommEventObserver observer) throws IOException
    {
        return eventObserverMap.remove(observer) != null;
    }

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                int ev = waitEvent(handle);
System.err.println("Event="+ev)                ;
                WinCommEvent event = new WinCommEvent(ev);
                WinCommStatus commStatus = null;
                WinCommError commError = null;
                WinCommStat commStat = null;
                for (CommEventObserver observer : eventObserverMap.keySet())
                {
                    for (CommEvent.Type type : eventObserverMap.get(observer))
                    {
                        switch (type)
                        {
                            case BREAK:
                                if (event.isBreakEvent())
                                {
                                    observer.commEvent(type);
                                }
                                break;
                            case CTS:
                                if (event.isCtsEvent())
                                {
                                    if (commStatus == null)
                                    {
                                        commStatus = getCommStatus();
                                    }
                                    observer.commSignalChange(type, commStatus.isCts());
                                }
                                break;
                            case DSR:
                                if (event.isDsrEvent())
                                {
                                    if (commStatus == null)
                                    {
                                        commStatus = getCommStatus();
                                    }
                                    observer.commSignalChange(type, commStatus.isDsr());
                                }
                                break;
                            case ERROR:
                                if (event.isErrorEvent())
                                {
                                    if (commError == null)
                                    {
                                        commStat = new WinCommStat();
                                        commError = getError(commStat);
                                    }
                                    observer.commError(commError, commStat);
                                }
                                break;
                            case RING:
                                if (event.isRingEvent())
                                {
                                    observer.commEvent(type);
                                }
                                break;
                            case RLSD:
                                if (event.isRlsdEvent())
                                {
                                    if (commStatus == null)
                                    {
                                        commStatus = getCommStatus();
                                    }
                                    observer.commSignalChange(type, commStatus.isRlsd());
                                }
                                break;
                            case CHAR:
                                if (event.isCharEvent())
                                {
                                    observer.commEvent(type);
                                }
                                break;
                            case FLAG:
                                if (event.isFlagEvent())
                                {
                                    observer.commEvent(type);
                                }
                                break;
                            case EMPTY:
                                if (event.isEmptyEvent())
                                {
                                    observer.commEvent(type);
                                }
                                break;
                        }
                    }
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    private native int waitEvent(long handle) throws IOException;

    private WinCommError getError(WinCommStat stat) throws IOException
    {
        int err = doGetError(handle, stat);
        return new WinCommError(err);
    }

    private native int doGetError(long handle, WinCommStat stat) throws IOException;

    public static void debug(boolean on)
    {
        setDebug(on);
    }

    private static native void setDebug(boolean on);

    public boolean isConnected()
    {
        return connected(handle);
    }

    private native boolean connected(long handle);
    
    @Override
    protected void implCloseChannel() throws IOException
    {
        if (eventObserverThread != null)
        {
            eventObserverThread.interrupt();
        }
        doClose(handle);
        handle = -1;
    }

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
    public WinCommStatus getCommStatus() throws IOException
    {
        return new WinCommStatus(this);
    }

    protected native int commStatus(long handle) throws IOException;

    @Override
    public InputStream getInputStream(int bufferSize)
    {
        return new SerialInputStream(this, bufferSize);
    }

    @Override
    public OutputStream getOutputStream(int bufferSize)
    {
        return new SerialOutputStream(this, bufferSize);
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

    private static class SerialOutputStream extends OutputStream
    {
        private WinSerialChannel channel;
        private ByteBuffer buffer;

        public SerialOutputStream(WinSerialChannel channel, int bufferSize)
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
            buffer.put((byte)b);
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

    }

    private static class SerialInputStream extends InputStream
    {
        private WinSerialChannel channel;
        private ByteBuffer buffer;
        private boolean online;

        public SerialInputStream(WinSerialChannel channel, int bufferSize)
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
                throw new IllegalArgumentException("Couldn't set mark for "+readlimit+" bytes");
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

        @Override
        public String toString()
        {
            return "SerialInputStream{" + "channel=" + channel + "buffer=" + buffer + '}';
        }


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
