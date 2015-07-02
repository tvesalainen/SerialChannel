package javax.comm;

public abstract class CommPort
{

    protected String name;

    public String getName()
    {
        return name;
    }

    public abstract java.io.InputStream getInputStream()
            throws java.io.IOException;

    public abstract java.io.OutputStream getOutputStream()
            throws java.io.IOException;

    public void close()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public abstract void enableReceiveThreshold(int thresh)
            throws UnsupportedCommOperationException;

    public abstract void disableReceiveThreshold();

    public abstract boolean isReceiveThresholdEnabled();

    public abstract int getReceiveThreshold();

    public abstract void enableReceiveTimeout(int rcvTimeout)
            throws UnsupportedCommOperationException;

    public abstract void disableReceiveTimeout();

    public abstract boolean isReceiveTimeoutEnabled();

    public abstract int getReceiveTimeout();

    public abstract void enableReceiveFraming(int framingByte)
            throws UnsupportedCommOperationException;

    public abstract void disableReceiveFraming();

    public abstract boolean isReceiveFramingEnabled();

    public abstract int getReceiveFramingByte();

    public abstract void setInputBufferSize(int size);

    public abstract int getInputBufferSize();

    public abstract void setOutputBufferSize(int size);

    public abstract int getOutputBufferSize();

}
