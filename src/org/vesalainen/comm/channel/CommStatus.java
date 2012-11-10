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
package org.vesalainen.comm.channel;

import java.io.IOException;

/**
 *
 * @author tkv
 */
public interface CommStatus
{
    /**
     * The CTS (clear-to-send) signal is on.
     */
    public boolean isCts();
    /**
     * The DSR (data-set-ready) signal is on.
     * @return
     */
    public boolean isDsr();
    /**
     * The ring indicator signal is on.
     * @return
     */
    public boolean isRing();
    /**
     * The RLSD (receive-line-signal-detect) signal is on.
     * @return
     */
    public boolean isRlsd();

    public void refresh() throws IOException;

}
