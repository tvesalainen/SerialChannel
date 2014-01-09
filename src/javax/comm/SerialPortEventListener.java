package javax.comm;

import java.util.EventListener;

public interface SerialPortEventListener extends EventListener
{

    public void serialEvent(SerialPortEvent ev);
}
