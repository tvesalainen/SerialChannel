package javax.comm;

public class PortInUseException extends Exception
{

    public java.lang.String currentOwner;

    public PortInUseException()
    {
    }

    public PortInUseException(String msg)
    {
        super(msg);
    }
}
