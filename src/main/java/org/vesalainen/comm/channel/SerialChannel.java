
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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.vesalainen.comm.channel.linux.LinuxSerialChannel;
import org.vesalainen.loader.LibraryLoader;
import org.vesalainen.loader.LibraryLoader.OS;
import static org.vesalainen.loader.LibraryLoader.getOS;

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
public abstract class SerialChannel extends AbstractSelectableChannel implements GatheringByteChannel, ScatteringByteChannel
{
    /**
     * Baud rate. Depends on used devices which are supported.
     */
    public enum Speed {B50, B75, B110, B134, B150, B200, B300, B600, B1200, B1800, B2400, B4800, B9600, B14400, B19200, B38400, B57600, B115200, B128000, B230400, B256000};
    public enum Parity {NONE, ODD, EVEN, MARK, SPACE};
    public enum DataBits {DATABITS_4, DATABITS_5, DATABITS_6, DATABITS_7, DATABITS_8};
    public enum StopBits {STOPBITS_10, STOPBITS_15, STOPBITS_20};
    public enum FlowControl {NONE, XONXOFF, RTSCTS, DSRDTR};

    protected long handle = -1;
    protected String port;
    protected Speed speed;
    protected Parity parity = Parity.NONE;
    protected StopBits stopBits = StopBits.STOPBITS_10;
    protected DataBits dataBits = DataBits.DATABITS_8;
    protected FlowControl flowControl = FlowControl.NONE;

    protected Thread eventObserverThread;
    
    protected boolean block = true;
    private boolean replaceError;

    protected SerialChannel()
    {
        super(SerialSelectorProvider.provider());
    }
    
    protected abstract int version();

    public static int select(Set<SelectionKey> keys, Set<SelectionKey> selected, int timeout) throws IOException
    {
        OS os = LibraryLoader.getOS();
        switch (os)
        {
            case Windows:
                return WinSerialChannel.doSelect(keys, selected, timeout);
            case Linux:
                return LinuxSerialChannel.doSelect(keys, selected, timeout);
            default:
                throw new UnsupportedOperationException(os+" not supported");
        }
    }
    
    public static void wakeupSelect(Set<SelectionKey> keys)
    {
        OS os = LibraryLoader.getOS();
        switch (os)
        {
            case Windows:
                WinSerialChannel.wakeupSelect(keys);
                break;
            case Linux:
                LinuxSerialChannel.wakeupSelect(keys);
                break;
            default:
                throw new UnsupportedOperationException(os+" not supported");
        }
    }
    @Override
    protected void implConfigureBlocking(boolean block) throws IOException
    {
        this.block = block;
        setTimeouts();
    }
    protected abstract void setTimeouts() throws IOException;
        
    public static int getSpeed(Speed speed)
    {
        return Integer.parseInt(speed.name().substring(1));
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
    protected void open() throws IOException
    {
        handle = doOpen(port.getBytes());
    }
    protected abstract long doOpen(byte[] port);
    
    public abstract byte[] getErrorReplacement();

    /**
     * Change channel configuration
     * @param builder
     * @throws IOException 
     */
    public void configure(Builder builder) throws IOException
    {
        this.speed = builder.speed;
        this.parity = builder.parity;
        this.stopBits = builder.stopBits;
        this.dataBits = builder.dataBits;
        this.flowControl = builder.flowControl;
        this.replaceError = builder.replaceError;
        doConfigure(
                handle,
                getSpeed(speed), 
                parity.ordinal(), 
                dataBits.ordinal(), 
                stopBits.ordinal(), 
                flowControl.ordinal(),
                replaceError
        );
    }

    protected abstract void doConfigure(
            long handle,
            int baudRate, 
            int parity, 
            int dataBits, 
            int stopBits, 
            int flowControl,
            boolean replaceError
    ) throws IOException;

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
     * Reads data at buffers position and then increments the position.
     * @param dst
     * @return Number of bytes read.
     * @throws IOException 
     */
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

    protected abstract int doRead(long handle, ByteBuffer dst) throws IOException;

    /**
     * Writes data at buffers position and then increments the position.
     * @param src
     * @return Returns number of characters written.
     * @throws IOException 
     */
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

    protected abstract int doWrite(long handle, ByteBuffer src) throws IOException;

    /**
     * Sets the debug state. When set to true, writes trace text to System.err
     * @param on 
     */
    public static void debug(boolean on)
    {
        OS os = LibraryLoader.getOS();
        switch (os)
        {
            case Windows:
                WinSerialChannel.setDebug(on);
                break;
            case Linux:
                LinuxSerialChannel.setDebug(on);
                break;
            default:
                throw new UnsupportedOperationException(os+" not supported");
        }
    }

    /**
     * Returns all ports that can be opened.
     * @return 
     */
    public static List<String> getFreePorts()
    {
        Builder builder = new Builder("", Speed.B57600);
        List<String> allPorts = getAllPorts();
        Iterator<String> iterator = allPorts.iterator();
        while (iterator.hasNext())
        {
            String port = iterator.next();
            builder.setPort(port);
            try (SerialChannel sc = builder.get())
            {
                
            }
            catch (IOException ex)
            {
                iterator.remove();
            }
        }
        return allPorts;
    }
    /**
     * Returns all available ports.
     * @return 
     */
    public static List<String> getAllPorts()
    {
        List<String> list = new ArrayList<>();
        OS os = LibraryLoader.getOS();
        switch (os)
        {
            case Windows:
                WinSerialChannel.doEnumPorts(list);;
                break;
            case Linux:
                LinuxSerialChannel.doEnumPorts(list);;
                break;
            default:
                throw new UnsupportedOperationException(os+" not supported");
        }
        return list;
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

    public boolean isReplaceError()
    {
        return replaceError;
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

    protected void doClose() throws IOException
    {
        doClose(handle);
        handle = -1;
    }

    protected abstract void doClose(long handle) throws IOException;

    
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
    
    public static class Builder
    {
        private String port;
        private Speed speed;
        private Parity parity = Parity.NONE;
        private StopBits stopBits = StopBits.STOPBITS_10;
        private DataBits dataBits = DataBits.DATABITS_8;
        private FlowControl flowControl = FlowControl.NONE;
        private boolean block = true;
        private boolean replaceError;

        public Builder(String port, Speed speed)
        {
            this.port = port;
            this.speed = speed;
        }

        public Builder(String port, int speed)
        {
            this.port = port;
            this.speed = Speed.valueOf("B"+speed);
        }

        public SerialChannel get() throws IOException
        {
            SerialChannel channel;
            switch (getOS())
            {
                case Windows:
                    channel = new WinSerialChannel(port);
                    break;
                case Linux:
                    channel = new LinuxSerialChannel(port);
                    break;
                default:
                    throw new UnsupportedOperationException("OS not supported");
            }
            channel.open();
            channel.configure(this);
            channel.configureBlocking(block);
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

        public Builder setReplaceError(boolean replace)
        {
            this.replaceError = replace;
            return this;
        }

        public String getPort()
        {
            return port;
        }

        public Speed getSpeed()
        {
            return speed;
        }

        public Parity getParity()
        {
            return parity;
        }

        public StopBits getStopBits()
        {
            return stopBits;
        }

        public DataBits getDataBits()
        {
            return dataBits;
        }

        public FlowControl getFlowControl()
        {
            return flowControl;
        }

        public boolean isBlock()
        {
            return block;
        }

        public boolean isReplaceError()
        {
            return replaceError;
        }
        
    }
}
