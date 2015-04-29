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
public interface CommError
{

    /**
     *
     * @return The hardware detected a break condition.
     */
    boolean isBreakCondition();

    /**
     *
     * @return The hardware detected a framing error.
     */
    boolean isFramingError();

    /**
     *
     * @return An input buffer overflow has occurred. There is either no room
     * in the input buffer, or a character was received after the end-of-file
     * (EOF) character.
     */
    boolean isInputBufferOverflow();

    /**
     *
     * @return A character-buffer overrun has occurred. The next character is lost.
     */
    boolean isOverRun();

    /**
     *
     * @return The hardware detected a parity error.
     */
    boolean isParityError();
}
