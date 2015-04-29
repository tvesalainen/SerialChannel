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
package org.vesalainen.comm.channel.winx;

import org.vesalainen.comm.channel.CommEvent;
import java.util.Set;

/**
 *
 * @author tkv
 */
public class WinCommEvent implements CommEvent
{
    private static final int BREAK = 0x0040;
    private static final int CTS = 0x0008;
    private static final int DSR = 0x0010;
    private static final int ERR = 0x0080;
    private static final int RING = 0x0100;
    private static final int RLSD = 0x0020;
    private static final int CHAR = 0x0001;
    private static final int FLAG = 0x0002;
    private static final int EMPTY = 0x0004;

    private int event;

    public WinCommEvent(int event)
    {
        this.event = event;
    }

    public static int createEventMask(Set<CommEvent.Type> set)
    {
        int mask = 0;
        for (CommEvent.Type type : set)
        {
            switch (type)
            {
                case BREAK:
                    mask |= BREAK;
                    break;
                case CTS:
                    mask |= CTS;
                    break;
                case DSR:
                    mask |= DSR;
                    break;
                case ERROR:
                    mask |= ERR;
                    break;
                case RING:
                    mask |= RING;
                    break;
                case RLSD:
                    mask |= RLSD;
                    break;
                case CHAR:
                    mask |= CHAR;
                    break;
                case FLAG:
                    mask |= FLAG;
                    break;
                case EMPTY:
                    mask |= EMPTY;
                    break;
            }
        }
        return mask;
    }

    
    public boolean isBreakEvent()
    {
        return (event & BREAK) != 0;
    }

    public boolean isCharEvent()
    {
        return (event & CHAR) != 0;
    }

    public boolean isCtsEvent()
    {
        return (event & CTS) != 0;
    }

    public boolean isDsrEvent()
    {
        return (event & DSR) != 0;
    }

    public boolean isEmptyEvent()
    {
        return (event & EMPTY) != 0;
    }

    public boolean isErrorEvent()
    {
        return (event & ERR) != 0;
    }

    public boolean isFlagEvent()
    {
        return (event & FLAG) != 0;
    }

    public boolean isRingEvent()
    {
        return (event & RING) != 0;
    }

    public boolean isRlsdEvent()
    {
        return (event & RLSD) != 0;
    }

}
