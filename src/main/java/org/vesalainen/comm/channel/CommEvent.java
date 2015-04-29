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
public interface CommEvent
{
    public enum Type {
        /**
         * A break was detected on input.
         */
        BREAK, 
        /**
         * The CTS (clear-to-send) signal changed state.
         */
        CTS, 
        /**
         * The DSR (data-set-ready) signal changed state.
         */
        DSR, 
        /**
         * A line-status error occurred. Line-status errors are CE_FRAME, CE_OVERRUN, and CE_RXPARITY.
         */
        ERROR, 
        /**
         * A ring indicator was detected.
         */
        RING, 
        /**
         * The RLSD (receive-line-signal-detect) signal changed state.
         */
        RLSD, 
        /**
         * A character was received and placed in the input buffer.
         */
        CHAR, 
        /**
         * The event character was received and placed in the input buffer.
         * The event character is specified in the device's DCB structure, which is
         * applied to a serial port by using the SetCommState function.
         */
        FLAG, 
        /**
         * The last character in the output buffer was sent.
         */
        EMPTY
    }
    /**
     *
     * @return A break was detected on input.
     */
    boolean isBreakEvent();

    /**
     *
     * @return A character was received and placed in the input buffer.
     */
    boolean isCharEvent();

    /**
     *
     * @return The CTS (clear-to-send) signal changed state.
     */
    boolean isCtsEvent();

    /**
     *
     * @return The DSR (data-set-ready) signal changed state.
     */
    boolean isDsrEvent();

    /**
     *
     * @return The last character in the output buffer was sent.
     */
    boolean isEmptyEvent();

    /**
     *
     * @return A line-status error occurred. Line-status errors are CE_FRAME, CE_OVERRUN, and CE_RXPARITY.
     */
    boolean isErrorEvent();

    /**
     *
     * @return The event character was received and placed in the input buffer.
     * The event character is specified in the device's DCB structure, which is
     * applied to a serial port by using the SetCommState function.
     */
    boolean isFlagEvent();

    /**
     *
     * @return A ring indicator was detected.
     */
    boolean isRingEvent();

    /**
     *
     * @return The RLSD (receive-line-signal-detect) signal changed state.
     */
    boolean isRlsdEvent();
}
