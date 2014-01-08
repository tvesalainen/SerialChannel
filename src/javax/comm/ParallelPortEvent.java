package javax.comm;

import java.util.EventObject;

public class ParallelPortEvent extends EventObject
{

    /**
     * @deprecated Replaced by getEventType method. For compatibility with
     * version 1.0 of the CommAPI only.
     */
    public int eventType;
    public static final int PAR_EV_BUFFER = 2;
    public static final int PAR_EV_ERROR = 1;

    public ParallelPortEvent(ParallelPort srcport,
            int eventtype,
            boolean oldvalue,
            boolean newvalue)
    {
        super(srcport);
        throw new UnsupportedOperationException("no supported");
    }

    public int getEventType()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public boolean getNewValue()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public boolean getOldValue()
    {
        throw new UnsupportedOperationException("no supported");
    }

}
