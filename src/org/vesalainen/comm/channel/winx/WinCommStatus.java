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

import org.vesalainen.comm.channel.CommStatus;
import java.io.IOException;

/**
 *
 * @author tkv
 */
public class WinCommStatus implements CommStatus
{
    private static final int CTS = 0x0010;
    private static final int DSR = 0x0020;
    private static final int RING = 0x0040;
    private static final int RLSD = 0x0080;

    private int status;
    private WinSerialChannel channel;

    protected WinCommStatus(WinSerialChannel channel) throws IOException
    {
        this.channel = channel;
        refresh();
    }

    public void refresh() throws IOException
    {
        this.status = channel.commStatus();
    }

    private native int getCommStatus(long handle) throws IOException;

    public boolean isCts()
    {
        return (status & CTS) != 0;
    }

    public boolean isDsr()
    {
        return (status & DSR) != 0;
    }

    public boolean isRing()
    {
        return (status & RING) != 0;
    }

    public boolean isRlsd()
    {
        return (status & RLSD) != 0;
    }

    @Override
    public String toString()
    {
        return "CTS="+isCts()+" DSR="+isDsr()+" RING="+isRing()+" RLSD="+isRlsd();
    }

}
