
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.vesalainen.comm.channel.SerialChannel.Builder;
import org.vesalainen.comm.channel.SerialChannel.Configuration;
import org.vesalainen.loader.LibraryLoader;
import org.vesalainen.loader.LibraryLoader.OS;
import org.vesalainen.util.OperatingSystem;
import org.vesalainen.util.logging.AttachedLogger;
import org.vesalainen.util.logging.JavaLogging;

/**
 * A class for making connection to a serial port E.g RS232. You make the connection 
 * by using SerialChannel.Builder
 * 
 * <p>
 * It is also possible to use Streams. Use getInputStream and getOutputStream.
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public abstract class SerialChannel<B extends Builder, C extends Configuration> extends AbstractSelectableChannel implements ByteChannel, GatheringByteChannel, ScatteringByteChannel, AttachedLogger
{
    /**
     * The maximum number of reads or writes in select
     */
    public static final int MaxSelectors = 64;
    /**
     * Baud rate. Depends on used devices which are supported.
     */
    public enum Speed {B50, B75, B110, B134, B150, B200, B300, B600, B1200, B1800, B2400, B4800, B9600, B14400, B19200, B38400, B57600, B115200, B128000, B230400, B256000};
    public enum Parity {NONE, ODD, EVEN, MARK, SPACE};
    public enum DataBits {DATABITS_4, DATABITS_5, DATABITS_6, DATABITS_7, DATABITS_8};
    public enum StopBits {STOPBITS_1, STOPBITS_1_5, STOPBITS_2};
    public enum FlowControl {NONE, XONXOFF, RTSCTS, DSRDTR};

    protected long address = -1;
    protected String port;
    protected Configuration configuration;

    protected boolean block = true;
    protected boolean clearOnClose;
    
    protected ReentrantLock readLock = new ReentrantLock();
    protected ReentrantLock writeLock = new ReentrantLock();
    
    protected static final JavaLogging log = new JavaLogging("org.vesalainen.comm.channel");

    protected SerialChannel()
    {
        super(SerialSelectorProvider.provider());
    }

    protected abstract int version();
    /**
     * Clears input and output buffers.
     */
    public void clearBuffers()
    {
        doClearBuffers(address);
    }
    
    protected abstract void doClearBuffers(long address);
    /**
     * Returns the current configuration
     * @return 
     */
    public Configuration getConfiguration()
    {
        return configuration;
    }

    public boolean isClearOnClose()
    {
        return clearOnClose;
    }

    public void setClearOnClose(boolean clearOnClose)
    {
        this.clearOnClose = clearOnClose;
    }

    static int select(Set<SelectionKey> keys, Set<SelectionKey> selected, int timeout) throws IOException
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
    
    static void wakeupSelect(Set<SelectionKey> keys)
    {
        log.fine("wakeupSelect(%s)", keys);
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
    /**
     * Return the speed
     * @param speed
     * @return 
     */    
    public static int getSpeed(Speed speed)
    {
        return Integer.parseInt(speed.name().substring(1));
    }
    /**
     * Return the speed
     * @param speed
     * @return 
     */    
    public static Speed getSpeed(int speed)
    {
        return Speed.valueOf("B"+speed);
    }
    public static DataBits getDataBits(int bits)
    {
        return DataBits.valueOf("DATABITS_"+bits);
    }
    public static StopBits getStopBits(int bits)
    {
        return StopBits.valueOf("STOPBITS_"+bits);
    }
    public static Parity getParity(String parity)
    {
        return Parity.valueOf(parity);
    }
    public static FlowControl getFlowControl(String flow)
    {
        return FlowControl.valueOf(flow);
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
        address = doOpen(port.getBytes());
        setTimeouts();
    }
    protected abstract long doOpen(byte[] port);
    
    public static byte[] getErrorReplacement()
    {
        OS os = LibraryLoader.getOS();
        switch (os)
        {
            case Windows:
                return WinSerialChannel.errorReplacement();
            case Linux:
                return LinuxSerialChannel.errorReplacement();
            default:
                throw new UnsupportedOperationException(os+" not supported");
        }
    }

    /**
     * Change channel configuration
     * @param config
     * @throws IOException 
     */
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
     * Returns InputStream. Allocates direct ByteBuffer bufferSize length. 
     * Note! closing the stream doesn't close the channel.
     * <p>Using streams is not allowed in non-blocking mode.
     * @param bufferSize
     * @return 
     * @throws IllegalStateException if in non-blocking mode.
     */
    public InputStream getInputStream(int bufferSize)
    {
        if (!block)
        {
            throw new IllegalStateException("not allowed in non-blocking mode");
        }
        return new SerialInputStream(this, bufferSize);
    }

    /**
     * Returns OutputStream. Allocates direct ByteBuffer bufferSize length. Note 
     * that write doesn't actually write anything before the buffer comes full.
     * Use flush to flush the buffer. Note! closing the stream doesn't close the 
     * channel.
     * <p>Using streams is not allowed in non-blocking mode.
     * @param bufferSize
     * @return 
     * @throws IllegalStateException if in non-blocking mode.
     */
    public OutputStream getOutputStream(int bufferSize)
    {
        if (!block)
        {
            throw new IllegalStateException("not allowed in non-blocking mode");
        }
        return new SerialOutputStream(this, bufferSize);
    }

    /**
     * Reads data at buffers position and then increments the position.
     * @param dst
     * @return Number of bytes read.
     * @throws IOException 
     */
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
        List<String> freePorts = new ArrayList<>(getAllPorts());
        Iterator<String> iterator = freePorts.iterator();
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
        return Collections.unmodifiableList(freePorts);
    }
    /**
     * Returns all available ports.
     * @return 
     */
    public static List<String> getAllPorts()
    {
        List<String> list = new ArrayList<>();
        OperatingSystem os = OperatingSystem.getOperatingSystem();
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
        return Collections.unmodifiableList(list);
    }

    public DataBits getDataBits()
    {
        return configuration.dataBits;
    }

    public FlowControl getFlowControl()
    {
        return configuration.flowControl;
    }

    public Parity getParity()
    {
        return configuration.parity;
    }

    public Speed getSpeed()
    {
        return configuration.speed;
    }

    public StopBits getStopBits()
    {
        return configuration.stopBits;
    }

    public boolean isReplaceError()
    {
        return configuration.replaceError;
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException
    {
        if (clearOnClose)
        {
            clearBuffers();
        }
        doClose();
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        if (address != -1)
        {
            free(address);
        }
    }

    protected void doClose() throws IOException
    {
        fine("doClose %s", this);
        doClose(address);
    }

    protected abstract void doClose(long handle) throws IOException;
    protected abstract void free(long handle);

    
    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
    {
        writeLock.lock();
        try
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
        finally
        {
            writeLock.unlock();
        }
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException
    {
        return write(srcs, 0, srcs.length);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
    {
        readLock.lock();
        try
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
        finally
        {
            readLock.unlock();
        }
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException
    {
        return read(dsts, 0, dsts.length);
    }
    
    public static Builder builder(String port)
    {
        return new Builder(port, new Configuration());
    }

    public static Builder builder(String port, int speed)
    {
        return builder(port, Speed.valueOf("B"+speed));
    }

    public static Builder builder(String port, Speed speed)
    {
        return builder(port).setSpeed(speed);
    }

    public static <C extends Configuration> Builder builder(String port, C configuration)
    {
        return builder(port).setConfiguration(configuration);
    }

    public static class Configuration
    {
        protected Speed speed;
        protected Parity parity = Parity.NONE;
        protected StopBits stopBits = StopBits.STOPBITS_1;
        protected DataBits dataBits = DataBits.DATABITS_8;
        protected FlowControl flowControl = FlowControl.NONE;
        protected boolean replaceError;
        
        public float getFrameSize()
        {
            float size = 1;   // start
            switch (parity)
            {
                case NONE:
                    break;
                default:
                    size++;
            }
            switch (dataBits)
            {
                case DATABITS_4:
                    size += 4;
                    break;
                case DATABITS_5:
                    size += 5;
                    break;
                case DATABITS_6:
                    size += 6;
                    break;
                case DATABITS_7:
                    size += 7;
                    break;
                case DATABITS_8:
                    size += 8;
                    break;
            }
            switch (stopBits)
            {
                case STOPBITS_1:
                    size += 1;
                    break;
                case STOPBITS_1_5:
                    size += 1.5;
                    break;
                case STOPBITS_2:
                    size += 2;
                    break;
            }
            return size;
        }
        public int getBytesPerSecond()
        {
            return (int) (SerialChannel.getSpeed(speed)/getFrameSize());
        }
        public Configuration setDataBits(DataBits dataBits)
        {
            this.dataBits = dataBits;
            return this;
        }

        public Configuration setFlowControl(FlowControl flowControl)
        {
            this.flowControl = flowControl;
            return this;
        }

        public Configuration setParity(Parity parity)
        {
            this.parity = parity;
            return this;
        }

        public Configuration setSpeed(Speed speed)
        {
            this.speed = speed;
            return this;
        }

        public Configuration setStopBits(StopBits stopBits)
        {
            this.stopBits = stopBits;
            return this;
        }

        public Configuration setReplaceError(boolean replace)
        {
            this.replaceError = replace;
            return this;
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

        public boolean isReplaceError()
        {
            return replaceError;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("baud="+SerialChannel.getSpeed(speed));
            switch (parity)
            {
                case NONE:
                    sb.append(" parity=N");
                    break;
                case EVEN:
                    sb.append(" parity=E");
                    break;
                case ODD:
                    sb.append(" parity=O");
                    break;
                case MARK:
                    sb.append(" parity=M");
                    break;
                case SPACE:
                    sb.append(" parity=S");
                    break;
            }
            switch (dataBits)
            {
                case DATABITS_4:
                    sb.append(" data=4");
                    break;
                case DATABITS_5:
                    sb.append(" data=5");
                    break;
                case DATABITS_6:
                    sb.append(" data=6");
                    break;
                case DATABITS_7:
                    sb.append(" data=7");
                    break;
                case DATABITS_8:
                    sb.append(" data=8");
                    break;
            }
            switch (stopBits)
            {
                case STOPBITS_1:
                    sb.append(" stop=1");
                    break;
                case STOPBITS_1_5:
                    sb.append(" stop=1.5");
                    break;
                case STOPBITS_2:
                    sb.append(" stop=2");
                    break;
            }
            switch (flowControl)
            {
                case NONE:
                    break;
                case XONXOFF:
                    sb.append(" flow=2");
                    break;
                case RTSCTS:
                    sb.append(" flow=RTS/CTS");
                    break;
                case DSRDTR:
                    sb.append(" flo=DSR/DTR");
                    break;
            }
            return sb.toString();
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.speed);
            hash = 89 * hash + Objects.hashCode(this.parity);
            hash = 89 * hash + Objects.hashCode(this.stopBits);
            hash = 89 * hash + Objects.hashCode(this.dataBits);
            hash = 89 * hash + Objects.hashCode(this.flowControl);
            hash = 89 * hash + (this.replaceError ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final Configuration other = (Configuration) obj;
            if (this.speed != other.speed)
            {
                return false;
            }
            if (this.parity != other.parity)
            {
                return false;
            }
            if (this.stopBits != other.stopBits)
            {
                return false;
            }
            if (this.dataBits != other.dataBits)
            {
                return false;
            }
            if (this.flowControl != other.flowControl)
            {
                return false;
            }
            if (this.replaceError != other.replaceError)
            {
                return false;
            }
            return true;
        }

    }
    /**
     * A class that is used to configure serial port and open it.
     */
    public static class Builder<C extends Configuration>
    {
        protected String port;
        protected boolean block = true;
        protected C configuration;
        /**
         * @deprecated Use SerialChannel builder
         * @param port
         * @param speed 
         * @see org.vesalainen.comm.channel.SerialChannel#builder(java.lang.String, int) 
         */
        public Builder(String port, int speed)
        {
            this(port, Speed.valueOf("B"+speed));
        }
        /**
         * @deprecated Use SerialChannel builder
         * @param port
         * @param speed 
         * @see org.vesalainen.comm.channel.SerialChannel#builder(java.lang.String, org.vesalainen.comm.channel.SerialChannel.Speed) 
         */
        public Builder(String port, Speed speed)
        {
            this.port = port;
            this.configuration = (C) new Configuration();
            this.configuration.setSpeed(speed);
        }
        /**
         * 
         * @param port
         * @param configuration 
         * @see org.vesalainen.comm.channel.SerialChannel#builder(java.lang.String, org.vesalainen.comm.channel.SerialChannel.Configuration) 
         */
        protected Builder(String port, C configuration)
        {
            this.port = port;
            this.configuration = configuration;
        }

        public Builder setConfiguration(C configuration)
        {
            this.configuration = configuration;
            return this;
        }
        /**
         * @deprecated Use build
         * @return
         * @throws IOException 
         * @see org.vesalainen.comm.channel.SerialChannel.Builder#build() 
         */
        public SerialChannel get() throws IOException
        {
            return build();
        }
        /**
         * Opens and configures new SerialChannel
         * @return
         * @throws IOException 
         */
        public SerialChannel build() throws IOException
        {
            SerialChannel channel;
            
            switch (OperatingSystem.getOperatingSystem())
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
            channel.configure(configuration);
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
        public Builder setBlocking(boolean block)
        {
            this.block = block;
            return this;
        }

        public String getPort()
        {
            return port;
        }

        public Speed getSpeed()
        {
            return configuration.speed;
        }

        public boolean isBlock()
        {
            return block;
        }

        public Builder setStopBits(StopBits stopBits)
        {
            configuration.setStopBits(stopBits);
            return this;
        }

        public Builder setSpeed(Speed speed)
        {
            configuration.setSpeed(speed);
            return this;
        }

        public Builder setParity(Parity parity)
        {
            configuration.setParity(parity);
            return this;
        }

        public Builder setFlowControl(FlowControl flowControl)
        {
            configuration.setFlowControl(flowControl);
            return this;
        }

        public Builder setDataBits(DataBits dataBits)
        {
            configuration.setDataBits(dataBits);
            return this;
        }

        public Builder setReplaceError(boolean replace)
        {
            configuration.setReplaceError(replace);
            return this;
        }

        public int getBytesPerSecond()
        {
            return configuration.getBytesPerSecond();
        }
        
    }
}
