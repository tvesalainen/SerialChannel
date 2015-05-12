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

import org.vesalainen.comm.channel.winx.WinSerialChannel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.vesalainen.comm.channel.winx.WinCommEvent;
import org.vesalainen.comm.channel.winx.WinCommStat;

/**
 * A class for making connection to a serial port E.g RS232. You make the connection 
 * by getting SerialChannel instance with getInstance method. After that call connect method.
 * Sending and receiving data is done either with ByteBuffer method read and write. 
 * This is the recommended way. It is also recommended to use direct buffers.
 * 
 * <p>
 * It is also possible to use Streams. Use getInputStream and getOutputStream.
 * @author tkv
 */
public abstract class SerialChannel extends AbstractSelectableChannel implements Runnable, GatheringByteChannel, ScatteringByteChannel
{

    public enum Speed {CBR_110, CBR_300, CBR_600, CBR_1200, CBR_2400, CBR_4800, CBR_9600, CBR_14400, CBR_19200, CBR_38400, CBR_57600, CBR_115200, CBR_128000, CBR_256000};
    protected static final int[] SPEED = new int[] {110, 300, 600, 1200, 2400, 4800, 9600, 14400, 19200, 38400, 57600, 115200, 128000, 256000};
    public enum Parity {NONE, ODD, EVEN, MARK, SPACE};
    public enum DataBits {DATABITS_4, DATABITS_5, DATABITS_6, DATABITS_7, DATABITS_8};
    public enum StopBits {STOPBITS_10, STOPBITS_15, STOPBITS_20};
    public enum FlowControl {NONE, XONXOFF, RTSCTS, DSRDTR};

    private enum OS {Windows, Linux};
    
    protected String port;
    protected Speed speed;
    protected Parity parity = Parity.NONE;
    protected StopBits stopBits = StopBits.STOPBITS_10;
    protected DataBits dataBits = DataBits.DATABITS_8;
    protected FlowControl flowControl = FlowControl.NONE;

    protected Map<CommEventObserver,Set<CommEvent.Type>> eventObserverMap;
    protected Set<CommEvent.Type> observedEvents;
    protected Thread eventObserverThread;
    
    protected boolean block = true;
    
    protected SerialChannel()
    {
        super(SerialSelectorProvider.provider());
    }
    private static OS getOS()
    {
        String osName = System.getProperty("os.name");
        if (osName.contains("indows"))
        {
            return OS.Windows;
        }
        throw new UnsupportedOperationException(osName+" not supported");
    }

    public static int select(Set<SelectionKey> keys, Set<SelectionKey> selected, int timeout) throws IOException
    {
        OS os = getOS();
        switch (os)
        {
            case Windows:
                return WinSerialChannel.doSelect(keys, selected, timeout);
            default:
                throw new UnsupportedOperationException(os+" not supported");
        }
    }
    
    @Override
    protected void implConfigureBlocking(boolean block) throws IOException
    {
        this.block = block;
    }

    /**
     * Returns the port.
     * @return 
     */
    public String getPort()
    {
        return port;
    }
    /**
     * Creates actual connection.
     * @throws IOException 
     */
    protected abstract void connect() throws IOException;
    /**
     * Flushes the buffers.
     * @throws IOException 
     */
    public abstract void flush() throws IOException;
    /**
     * Returns InputStream. Allocates direct ByteBuffer bufferSize length. Note! closing the stream doesn't close the 
     * channel.
     * @param bufferSize
     * @return 
     */
    public InputStream getInputStream(int bufferSize)
    {
        return new SerialInputStream(this, bufferSize);
    }

    /**
     * Returns OutputStream. Allocates direct ByteBuffer bufferSize length. Note 
     * that write doesn't actually write anything before the buffer comes full.
     * Use flush to flush the buffer. Note! closing the stream doesn't close the 
     * channel.
     * @param bufferSize
     * @return 
     */
    public OutputStream getOutputStream(int bufferSize)
    {
        return new SerialOutputStream(this, bufferSize);
    }

    /**
     * Returns true if channel is connected.
     * @return 
     */
    public abstract boolean isConnected();
    /**
     * Reads data at buffers position and then increments the position.
     * @param dst
     * @return Number of bytes read.
     * @throws IOException 
     */
    @Override
    public abstract int read(ByteBuffer dst) throws IOException;
    /**
     * Writes data at buffers position and then increments the position.
     * @param src
     * @return Returns number of characters written.
     * @throws IOException 
     */
    @Override
    public abstract int write(ByteBuffer src) throws IOException;
    /**
     * Returns the status.
     * @return
     * @throws IOException 
     */
    public abstract CommStatus getCommStatus() throws IOException;
    /**
     * Sets the debug state. When set to true, writes trace text to System.err
     * @param on 
     */
    public static void debug(boolean on)
    {
        switch (getOS())
        {
            case Windows:
                WinSerialChannel.debug(on);
            default:
                throw new UnsupportedOperationException("OS not supported");
        }
    }
    /**
     * Returns all available ports.
     * @return 
     */
    public static List<String> getAllPorts()
    {
        return WinSerialChannel.getAllPorts();
    }

    public DataBits getDataBits()
    {
        return dataBits;
    }

    public FlowControl getFlowControl()
    {
        return flowControl;
    }

    public Parity getParity()
    {
        return parity;
    }

    public Speed getSpeed()
    {
        return speed;
    }

    public StopBits getStopBits()
    {
        return stopBits;
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException
    {
        if (eventObserverThread != null)
        {
            eventObserverThread.interrupt();
        }
        doClose();
    }

    protected abstract void doClose() throws IOException;
    
    /**
     * Adds an event observer for given events.
     * @param observer
     * @param types
     * @throws IOException 
     */
    public void addEventObserver(CommEventObserver observer, CommEvent.Type... types) throws IOException
    {
        if (eventObserverMap == null)
        {
            eventObserverMap = new HashMap<>();
            observedEvents = EnumSet.noneOf(CommEvent.Type.class);
        }
        Set<CommEvent.Type> set = EnumSet.copyOf(Arrays.asList(types));
        eventObserverMap.put(observer, set);
        observedEvents.addAll(set);
        setEventMask(WinCommEvent.createEventMask(observedEvents));
        if (eventObserverThread == null && !observedEvents.isEmpty())
        {
            eventObserverThread = new Thread(this);
            eventObserverThread.start();
        }
    }

    protected abstract void setEventMask(int mask) throws IOException;

    protected abstract int waitEvent(int mask) throws IOException;

    /**
     * Removes event observer.
     * @param observer
     * @return
     * @throws IOException 
     */
    public boolean removeEventObserver(CommEventObserver observer) throws IOException
    {
        return eventObserverMap.remove(observer) != null;
    }

    protected abstract CommError getError(CommStat stat) throws IOException;

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
    {
        long res = 0;
        for  (int ii=0;ii<length;ii++)
        {
            ByteBuffer bb = srcs[ii+offset];
            if (bb.hasRemaining())
            {
                res += write(bb);
                if (bb.hasRemaining())
                {
                    break;
                }
            }
        }
        return res;
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException
    {
        return write(srcs, 0, srcs.length);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
    {
        long res = 0;
        for  (int ii=0;ii<length;ii++)
        {
            ByteBuffer bb = dsts[ii+offset];
            if (bb.hasRemaining())
            {
                int rc = read(bb);
                if (rc == -1)
                {
                    if (res == 0)
                    {
                        return -1;
                    }
                    else
                    {
                        return res;
                    }
                }
                res += rc;
                if (bb.hasRemaining())
                {
                    break;
                }
            }
        }
        return res;
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException
    {
        return read(dsts, 0, dsts.length);
    }
    
    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                int ev = waitEvent(0);
                WinCommEvent event = new WinCommEvent(ev);
                CommStatus commStatus = null;
                CommError commError = null;
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

    public static class Builder
    {
        private String port;
        private Speed speed;
        private Parity parity = Parity.NONE;
        private StopBits stopBits = StopBits.STOPBITS_10;
        private DataBits dataBits = DataBits.DATABITS_8;
        private FlowControl flowControl = FlowControl.NONE;
        private boolean block = true;

        public Builder(String port, Speed speed)
        {
            this.port = port;
            this.speed = speed;
        }

        public Builder(String port, int speed)
        {
            this.port = port;
            this.speed = Speed.valueOf("CBR_"+speed);
        }

        public SerialChannel get() throws IOException
        {
            SerialChannel channel;
            switch (getOS())
            {
                case Windows:
                    channel = new WinSerialChannel(port, speed, parity, stopBits, dataBits, flowControl);
                    break;
                default:
                    throw new UnsupportedOperationException("OS not supported");
            }
            channel.configureBlocking(block);
            channel.connect();
            return channel;
        }
        /**
         * Sets the port.
         * @param port 
         * @return  
         */
        public Builder setPort(String port)
        {
            this.port = port;
            return this;
        }
        public Builder setDataBits(DataBits dataBits)
        {
            this.dataBits = dataBits;
            return this;
        }

        public Builder setFlowControl(FlowControl flowControl)
        {
            this.flowControl = flowControl;
            return this;
        }

        public Builder setParity(Parity parity)
        {
            this.parity = parity;
            return this;
        }

        public Builder setSpeed(Speed speed)
        {
            this.speed = speed;
            return this;
        }

        public Builder setStopBits(StopBits stopBits)
        {
            this.stopBits = stopBits;
            return this;
        }

        public Builder setBlocking(boolean block)
        {
            this.block = block;
            return this;
        }
    }
}
