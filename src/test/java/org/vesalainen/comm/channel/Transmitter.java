/*
 * Copyright (C) 2015 tkv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import java.io.OutputStream;
import java.util.concurrent.Callable;

/**
 *
 * @author tkv
 */
public class Transmitter implements Callable<Void>
{
    private final SerialChannel channel;
    private final int count;

    public Transmitter(SerialChannel channel, int count)
    {
        this.channel = channel;
        this.count = count;
    }

    @Override
    public Void call() throws Exception
    {
        int bits = channel.getDataBits().ordinal() + 4;
        RandomChar rand = new RandomChar();
        try (final OutputStream os = channel.getOutputStream(70))
        {
            for (int ii = 0; ii < count; ii++)
            {
                int next = rand.next(bits);
                os.write(next);
            }
            os.flush();
            System.err.println("transmitted all");
        }
        return null;
    }
    
}
