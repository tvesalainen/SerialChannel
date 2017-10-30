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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.vesalainen.util.logging.AttachedLogger;

/**
 *
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class LogPortMonitor extends PortMonitor implements AttachedLogger
{

    public LogPortMonitor()
    {
        init();
    }

    public LogPortMonitor(long period, TimeUnit unit)
    {
        super(period, unit);
        init();
    }

    public LogPortMonitor(ScheduledExecutorService scheduler, long period, TimeUnit unit)
    {
        super(scheduler, period, unit);
        init();
    }

    private void init()
    {
        addNewPortConsumer((p)->info("new port %s", p));
        addNewFreePortConsumer((p)->info("new free port %s", p));
        addRemovePortConsumer((p)->info("removed port %s", p));
        addRemoveFreePortConsumer((p)->info("removed free port %s", p));
    }
    
}
