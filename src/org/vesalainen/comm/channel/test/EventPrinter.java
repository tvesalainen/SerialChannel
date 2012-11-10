/*
 * Copyright (C) 2011 Timo Vesalainen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.vesalainen.comm.channel.test;

import org.vesalainen.comm.channel.CommError;
import org.vesalainen.comm.channel.CommEvent.Type;
import org.vesalainen.comm.channel.CommEventObserver;
import org.vesalainen.comm.channel.CommStat;

/**
 * CommEventObserver that prints event to System.err
 * @author tkv
 */
public class EventPrinter implements CommEventObserver
{

    @Override
    public void commEvent(Type event)
    {
        System.err.println(event);
    }

    @Override
    public void commSignalChange(Type event, boolean newState)
    {
        System.err.println(event+"="+newState);
    }

    @Override
    public void commError(CommError error, CommStat stat)
    {
        System.err.println("Error="+error+" stat="+stat);
    }
}
