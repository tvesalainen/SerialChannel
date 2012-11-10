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

import org.vesalainen.comm.channel.CommStat;

/**
 *
 * @author tkv
 */
public class WinCommStat implements CommStat
{
    private boolean ctsHold;
    private boolean dsrHold;
    private boolean rlsdHold;
    private boolean xoffHold;
    private boolean xoffSent;
    private boolean eof;
    private boolean txim;
    private int inQueue;
    private int outQueue;

    public boolean isCtsHold()
    {
        return ctsHold;
    }

    public boolean isDsrHold()
    {
        return dsrHold;
    }

    public boolean isEof()
    {
        return eof;
    }

    public int getInQueue()
    {
        return inQueue;
    }

    public int getOutQueue()
    {
        return outQueue;
    }

    public boolean isRlsdHold()
    {
        return rlsdHold;
    }

    public boolean isTxim()
    {
        return txim;
    }

    public boolean isXoffHold()
    {
        return xoffHold;
    }

    public boolean isXoffSent()
    {
        return xoffSent;
    }

}
