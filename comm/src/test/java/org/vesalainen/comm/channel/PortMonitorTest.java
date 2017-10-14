/*
 * Copyright (C) 2017 Timo Vesalainen <timo.vesalainen@iki.fi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.vesalainen.comm.channel;

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class PortMonitorTest
{
    
    public PortMonitorTest()
    {
    }

    //@Test
    public void test() throws InterruptedException
    {
        PortMonitor m = new PortMonitor(10, TimeUnit.SECONDS);
        m.addNewPortConsumer((p)->System.err.println("new "+p));
        m.addNewFreePortConsumer((p)->System.err.println("free "+p));
        m.addRemovePortConsumer((p)->System.err.println("remove "+p));
        Thread.sleep(100000);
    }
    
}
