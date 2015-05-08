package javax.comm;

public interface CommDriver
{

    public void initialize();

    public CommPort getCommPort(String portName, int portType);
}
