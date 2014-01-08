package javax.comm;

public class CommPortIdentifier
{

    public static final int PORT_PARALLEL = 2;
    public static final int PORT_SERIAL = 1;

    public CommPortIdentifier(java.lang.String name,
            CommPort port,
            int type,
            CommDriver driver)
    {
        throw new UnsupportedOperationException("no supported");
    }

    public static java.util.Enumeration getPortIdentifiers()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public static CommPortIdentifier getPortIdentifier(java.lang.String portName)
            throws NoSuchPortException
    {
        throw new UnsupportedOperationException("no supported");
    }

    public static CommPortIdentifier getPortIdentifier(CommPort port)
            throws NoSuchPortException
    {
        throw new UnsupportedOperationException("no supported");
    }

    public static void addPortName(java.lang.String portName,
            int portType,
            CommDriver driver)
    {
        throw new UnsupportedOperationException("no supported");
    }

    public CommPort open(java.lang.String appname,
            int timeout)
            throws PortInUseException
    {
        throw new UnsupportedOperationException("no supported");
    }

    public CommPort open(java.io.FileDescriptor fd)
            throws UnsupportedCommOperationException
    {
        throw new UnsupportedOperationException("no supported");
    }

    public java.lang.String getName()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public int getPortType()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public java.lang.String getCurrentOwner()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public boolean isCurrentlyOwned()
    {
        throw new UnsupportedOperationException("no supported");
    }

    public void addPortOwnershipListener(CommPortOwnershipListener listener)
    {
        throw new UnsupportedOperationException("no supported");
    }

    public void removePortOwnershipListener(CommPortOwnershipListener listener)
    {
        throw new UnsupportedOperationException("no supported");
    }
}
