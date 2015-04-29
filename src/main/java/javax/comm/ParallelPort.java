package javax.comm;

public abstract class ParallelPort extends CommPort
{

    public static final int LPT_MODE_ANY = 0;
    public static final int LPT_MODE_ECP = 4;
    public static final int LPT_MODE_EPP = 3;
    public static final int LPT_MODE_NIBBLE = 5;
    public static final int LPT_MODE_PS2 = 2;
    public static final int LPT_MODE_SPP = 1;

    public ParallelPort()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public abstract void addEventListener(ParallelPortEventListener lsnr)
            throws java.util.TooManyListenersException;

    public abstract void removeEventListener();

    public abstract void notifyOnError(boolean notify);

    public abstract void notifyOnBuffer(boolean notify);

    public abstract int getOutputBufferFree();

    public abstract boolean isPaperOut();

    public abstract boolean isPrinterBusy();

    public abstract boolean isPrinterSelected();

    public abstract boolean isPrinterTimedOut();

    public abstract int getMode();

    public abstract int setMode(int mode)
            throws UnsupportedCommOperationException;
}
