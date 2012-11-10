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

/**
 *
 * @author tkv
 */
public interface CommEventObserver
{
    /**
     * Reports BREAK, RING, CHAR, FLAG and EMPTY events
     * @param event
     */
    public void commEvent(CommEvent.Type event);
    /**
     * Reports CTS, DSR and RLSD events
     * @param event
     * @param newState
     */
    public void commSignalChange(CommEvent.Type event, boolean newState);
    /**
     * Reports errors
     * @param event
     * @param error
     * @param stat
     */
    public void commError(CommError error, CommStat stat);
}
