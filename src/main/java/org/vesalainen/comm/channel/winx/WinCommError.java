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

import org.vesalainen.comm.channel.CommError;

/**
 *
 * @author tkv
 */
public class WinCommError implements CommError
{
    private static final int BREAK = 0x0010;
    private static final int FRAME = 0x0008;
    private static final int OVERRUN = 0x0002;
    private static final int RXOVER = 0x0001;
    private static final int RXPARITY = 0x0004;

    private int error;

    public WinCommError(int error)
    {
        this.error = error;
    }

    public boolean isBreakCondition()
    {
        return (error & BREAK) != 0;
    }

    public boolean isFramingError()
    {
        return (error & FRAME) != 0;
    }

    public boolean isInputBufferOverflow()
    {
        return (error & RXOVER) != 0;
    }

    public boolean isOverRun()
    {
        return (error & OVERRUN) != 0;
    }

    public boolean isParityError()
    {
        return (error & RXPARITY) != 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Error(");
        if (isBreakCondition())
        {
            sb.append(" break");
        }
        if (isFramingError())
        {
            sb.append(" frame");
        }
        if (isInputBufferOverflow())
        {
            sb.append(" inOverrun");
        }
        if (isOverRun())
        {
            sb.append(" overrun");
        }
        if (isParityError())
        {
            sb.append(" parity");
        }
        sb.append(")");
        return sb.toString();
    }

}
