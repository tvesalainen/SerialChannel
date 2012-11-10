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
public interface CommStat {
    /**
     *
     * @return The number of bytes received by the serial provider but not yet
     * read by a ReadFile operation.
     */
    int getInQueue();
    /**
     *
     * @return The number of bytes received by the serial provider but not yet
     * read by a ReadFile operation.
     */
    int getOutQueue();
    /**
     *
     * @return If this member is TRUE, transmission is waiting for the CTS
     * (clear-to-send) signal to be sent.
     */
    boolean isCtsHold();
    /**
     *
     * @return If this member is TRUE, transmission is waiting for the DSR
     * (data-set-ready) signal to be sent.
     */
    boolean isDsrHold();
    /**
     *
     * @return If this member is TRUE, the end-of-file (EOF) character has been
     * received.
     */
    boolean isEof();
    /**
     *
     * @return If this member is TRUE, transmission is waiting for the RLSD
     * (receive-line-signal-detect) signal to be sent.
     */
    boolean isRlsdHold();
    /**
     *
     * @return If this member is TRUE, there is a character queued for
     * transmission that has come to the communications device by way of the
     * TransmitCommChar function. The communications device transmits such a
     * character ahead of other characters in the device's output buffer.
     */
    boolean isTxim();
    /**
     *
     * @return If this member is TRUE, transmission is waiting because the XOFF
     * character was received.
     */
    boolean isXoffHold();
    /**
     *
     * @return If this member is TRUE, transmission is waiting because the XOFF
     * character was transmitted. (Transmission halts when the XOFF character
     * is transmitted to a system that takes the next character as XON,
     * regardless of the actual character.)
     */
    boolean isXoffSent();

}
