package javax.comm;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import static javax.comm.CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED;
import org.vesalainen.comm.channel.SerialChannel;
import org.vesalainen.comm.channel.SerialChannel.Builder;
import org.vesalainen.comm.channel.SerialChannel.Configuration;
import static org.vesalainen.comm.channel.SerialChannel.Speed.B9600;

public class CommPortIdentifier
{

    public static final int PORT_PARALLEL = 2;
    public static final int PORT_SERIAL = 1;
    
    private final String port;
    private String owner;
    private final static Map<String,CommPortIdentifier> portMap = new WeakHashMap<>();
    private final static Map<CommPort,CommPortIdentifier> commPortMap = new WeakHashMap<>();
    private final List<CommPortOwnershipListener> listeners = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private CommPort commPort;
    private boolean requesting;

    public CommPortIdentifier(String name,
            CommPort port,
            int type,
            CommDriver driver)
    {
        this.port = name;
    }

    public static java.util.Enumeration getPortIdentifiers()
    {
        List<CommPortIdentifier> list = new ArrayList<>();
        for (String port : SerialChannel.getAllPorts())
        {
            CommPortIdentifier cpi = portMap.get(port);
            if (cpi == null)
            {
                cpi = new CommPortIdentifier(port, null, PORT_SERIAL, null);
                portMap.put(port, cpi);
            }
            list.add(cpi);
        }
        return Collections.enumeration(list);
    }

    public static CommPortIdentifier getPortIdentifier(String portName)
            throws NoSuchPortException
    {
        List<String> allPorts = SerialChannel.getAllPorts();
        if (allPorts.contains(portName))
        {
            CommPortIdentifier cpi = portMap.get(portName);
            if (cpi == null)
            {
                cpi = new CommPortIdentifier(portName, null, PORT_SERIAL, null);
                portMap.put(portName, cpi);
            }
            return cpi;
        }
        throw new NoSuchPortException(portName);
    }

    public static CommPortIdentifier getPortIdentifier(CommPort port)
            throws NoSuchPortException
    {
        CommPortIdentifier cpi = commPortMap.get(port);
        if (cpi != null)
        {
            return cpi;
        }
        throw new NoSuchPortException();
    }

    public static void addPortName(String portName,
            int portType,
            CommDriver driver)
    {
        throw new UnsupportedOperationException("not supported");
    }

    public CommPort open(String appname,
            int timeout)
            throws PortInUseException
    {
        lock.lock();
        try
        {
            if (owner == null)
            {
                owner = appname;
                commPort = new SerialPortImpl();
                return commPort;
            }
            if (owner.equals(appname))
            {
                return commPort;
            }
            requesting = true;
            fireListener(PORT_OWNERSHIP_REQUESTED);
            if (owner == null)
            {
                owner = appname;
                commPort = new SerialPortImpl();
                return commPort;
            }
            else
            {
                throw new PortInUseException(port);
            }
        }
        finally
        {
            lock.unlock();
        }
    }

    public CommPort open(FileDescriptor fd)
            throws UnsupportedCommOperationException
    {
        throw new UnsupportedOperationException("not supported");
    }

    public java.lang.String getName()
    {
        return port;
    }

    public int getPortType()
    {
        return PORT_SERIAL;
    }

    public String getCurrentOwner()
    {
        return owner;
    }

    public boolean isCurrentlyOwned()
    {
        return owner != null;
    }

    public void addPortOwnershipListener(CommPortOwnershipListener listener)
    {
        listeners.add(listener);
    }

    public void removePortOwnershipListener(CommPortOwnershipListener listener)
    {
        listeners.remove(listener);
    }
    private void fireListener(int event)
    {
        for (CommPortOwnershipListener l : listeners)
        {
            l.ownershipChange(event);
        }
    }

    public class SerialPortImpl extends SerialPort
    {
        private final Configuration config;
        private SerialChannel channel;
        private int inputBufferSize  = 4096;
        private int outputBufferSize  = 4096;
        private int rcvTimeout = -1;
        private int thresh = -1;
        private InputStream in;
        private OutputStream out;
        public SerialPortImpl()
        {
            this.name = port;
            config = new Configuration();
            config.setSpeed(B9600);
        }

        @Override
        public int getBaudRate()
        {
            return SerialChannel.getSpeed(config.getSpeed());
        }

        @Override
        public int getDataBits()
        {
            switch (config.getDataBits())
            {
                case DATABITS_5:
                    return SerialPort.DATABITS_5;
                case DATABITS_6:
                    return SerialPort.DATABITS_6;
                case DATABITS_7:
                    return SerialPort.DATABITS_7;
                case DATABITS_8:
                    return SerialPort.DATABITS_8;
                default:
                    throw new UnsupportedOperationException(config.getDataBits()+" not supported");
            }
        }

        @Override
        public int getStopBits()
        {
            switch (config.getStopBits())
            {
                case STOPBITS_1:
                    return SerialPort.STOPBITS_1;
                case STOPBITS_2:
                    return SerialPort.STOPBITS_2;
                case STOPBITS_1_5:
                    return SerialPort.STOPBITS_1_5;
                default:
                    throw new UnsupportedOperationException(config.getStopBits()+" not supported");
            }
        }

        @Override
        public int getParity()
        {
            switch (config.getParity())
            {
                case NONE:
                    return SerialPort.PARITY_NONE;
                case EVEN:
                    return SerialPort.PARITY_EVEN;
                case ODD:
                    return SerialPort.PARITY_ODD;
                case SPACE:
                    return SerialPort.PARITY_SPACE;
                case MARK:
                    return SerialPort.PARITY_MARK;
                default:
                    throw new UnsupportedOperationException(config.getParity()+" not supported");
            }
        }

        @Override
        public void sendBreak(int millis)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setFlowControlMode(int flowcontrol) throws UnsupportedCommOperationException
        {
            switch (flowcontrol)
            {
                case SerialPort.FLOWCONTROL_NONE:
                    config.setFlowControl(SerialChannel.FlowControl.NONE);
                    break;
                case SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT:
                    config.setFlowControl(SerialChannel.FlowControl.RTSCTS);
                    break;
                case SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT:
                    config.setFlowControl(SerialChannel.FlowControl.XONXOFF);
                    break;
                default:
                    throw new UnsupportedCommOperationException(flowcontrol+" not supported");
            }
        }

        @Override
        public int getFlowControlMode()
        {
            switch (config.getFlowControl())
            {
                case NONE:
                    return SerialPort.FLOWCONTROL_NONE;
                case RTSCTS:
                    return SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT;
                case XONXOFF:
                    return SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT;
                default:
                    throw new UnsupportedOperationException(config.getFlowControl()+" not supported");
            }
        }

        @Override
        public void setSerialPortParams(int baudrate, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException
        {
            try
            {
                config.setSpeed(SerialChannel.getSpeed(baudrate));
                switch (dataBits)
                {
                    case SerialPort.DATABITS_5:
                        config.setDataBits(SerialChannel.DataBits.DATABITS_5);
                        break;
                    case SerialPort.DATABITS_6:
                        config.setDataBits(SerialChannel.DataBits.DATABITS_6);
                        break;
                    case SerialPort.DATABITS_7:
                        config.setDataBits(SerialChannel.DataBits.DATABITS_7);
                        break;
                    case SerialPort.DATABITS_8:
                        config.setDataBits(SerialChannel.DataBits.DATABITS_8);
                        break;
                    default:
                        throw new UnsupportedCommOperationException("dataBits "+dataBits+" not supported");
                }
                switch (stopBits)
                {
                    case SerialPort.STOPBITS_1:
                        config.setStopBits(SerialChannel.StopBits.STOPBITS_1);
                        break;
                    case SerialPort.STOPBITS_2:
                        config.setStopBits(SerialChannel.StopBits.STOPBITS_2);
                        break;
                    case SerialPort.STOPBITS_1_5:
                        config.setStopBits(SerialChannel.StopBits.STOPBITS_1_5);
                        break;
                    default:
                        throw new UnsupportedCommOperationException("stopBits "+stopBits+" not supported");
                }
                switch (parity)
                {
                    case SerialPort.PARITY_NONE:
                        config.setParity(SerialChannel.Parity.NONE);
                        break;
                    case SerialPort.PARITY_EVEN:
                        config.setParity(SerialChannel.Parity.EVEN);
                        break;
                    case SerialPort.PARITY_ODD:
                        config.setParity(SerialChannel.Parity.ODD);
                        break;
                    case SerialPort.PARITY_SPACE:
                        config.setParity(SerialChannel.Parity.SPACE);
                        break;
                    case SerialPort.PARITY_MARK:
                        config.setParity(SerialChannel.Parity.MARK);
                        break;
                    default:
                        throw new UnsupportedCommOperationException("parity "+parity+" not supported");
                }
            }
            catch (Exception ex)
            {
                throw new UnsupportedCommOperationException(ex.getMessage());
            }
        }

        @Override
        public void setDTR(boolean dtr)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDTR()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setRTS(boolean rts)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRTS()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isCTS()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDSR()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRI()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isCD()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addEventListener(SerialPortEventListener lsnr) throws TooManyListenersException
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeEventListener()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnDataAvailable(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnOutputEmpty(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnCTS(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnDSR(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnRingIndicator(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnCarrierDetect(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnOverrunError(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnParityError(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnFramingError(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notifyOnBreakInterrupt(boolean enable)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            ensureOpen();
            if (in == null)
            {
                in = channel.getInputStream(inputBufferSize);
            }
            return in;
        }

        @Override
        public OutputStream getOutputStream() throws IOException
        {
            ensureOpen();
            if (out == null)
            {
                out = channel.getOutputStream(outputBufferSize);
            }
            return out;
        }

        @Override
        public void enableReceiveThreshold(int thresh) throws UnsupportedCommOperationException
        {
            if (thresh == 0)
            {
                this.thresh = thresh;
            }
            else
            {
                throw new UnsupportedCommOperationException("Supported only for 0 thresh.");
            }
        }

        @Override
        public void disableReceiveThreshold()
        {
            this.thresh = -1;
        }

        @Override
        public boolean isReceiveThresholdEnabled()
        {
            return thresh != -1;
        }

        @Override
        public int getReceiveThreshold()
        {
            return thresh;
        }

        @Override
        public void enableReceiveTimeout(int rcvTimeout) throws UnsupportedCommOperationException
        {
            if (rcvTimeout == 0)
            {
                this.rcvTimeout = rcvTimeout;
            }
            else
            {
                throw new UnsupportedCommOperationException("Supported only for 0 timeout.");
            }
        }

        @Override
        public void disableReceiveTimeout()
        {
            this.rcvTimeout = -1;
        }

        @Override
        public boolean isReceiveTimeoutEnabled()
        {
            return rcvTimeout != -1;
        }

        @Override
        public int getReceiveTimeout()
        {
            return rcvTimeout;
        }

        @Override
        public void enableReceiveFraming(int framingByte) throws UnsupportedCommOperationException
        {
            throw new UnsupportedCommOperationException("Not supported yet.");
        }

        @Override
        public void disableReceiveFraming()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isReceiveFramingEnabled()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getReceiveFramingByte()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setInputBufferSize(int size)
        {
            this.inputBufferSize = size;
        }

        @Override
        public int getInputBufferSize()
        {
            return inputBufferSize;
        }

        @Override
        public void setOutputBufferSize(int size)
        {
            this.outputBufferSize = size;
        }

        @Override
        public int getOutputBufferSize()
        {
            return outputBufferSize;
        }

        private void ensureOpen() throws IOException
        {
            if (channel == null)
            {
                Builder builder = new Builder(name, config);
                channel = builder.get();
            }
        }

    }
}
