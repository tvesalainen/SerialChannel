package javax.comm;

import java.util.EventListener;

public interface ParallelPortEventListener extends EventListener
{
    public void parallelEvent(ParallelPortEvent ev);
}
