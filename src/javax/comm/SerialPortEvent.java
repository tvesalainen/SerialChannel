package javax.comm;

import java.util.EventObject;

public class SerialPortEvent extends EventObject
{

    public static final int BI = 10;
    public static final int CD = 6;
    public static final int CTS = 3;
    public static final int DATA_AVAILABLE = 1;
    public static final int DSR = 4;
    public static final int FE = 9;
    public static final int OE = 7;
    public static final int OUTPUT_BUFFER_EMPTY = 2;
    public static final int PE = 8;
    public static final int RI = 5;

    public SerialPortEvent(SerialPort srcport,
            int eventtype,
            boolean oldvalue,
            boolean newvalue)
    {
        super(srcport);
        throw new UnsupportedOperationException("not supported");
    }

    public int getEventType()
    {
        throw new UnsupportedOperationException("not supported");
    }

    public boolean getNewValue()
    {
        throw new UnsupportedOperationException("not supported");
    }

    public boolean getOldValue()
    {
        throw new UnsupportedOperationException("not supported");
    }
}
