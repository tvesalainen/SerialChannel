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

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

/**
 *
 * @author tkv
 */
public class Receiver implements Callable<Integer>
{
    private final SerialChannel channel;
    private final ByteBuffer bb = ByteBuffer.allocateDirect(100);
    private final int count;

    public Receiver(SerialChannel channel, int count)
    {
        this.channel = channel;
        this.count = count;
    }

    @Override
    public Integer call() throws Exception
    {
        int errors = 0;
        int bits = channel.getDataBits().ordinal() + 4;
        RandomChar rand = new RandomChar(bits);
        channel.read(bb);
        bb.flip();
        for (int ii = 0; ii < count; ii++)
        {
            if (!bb.hasRemaining())
            {
                bb.clear();
                channel.read(bb);
                bb.flip();
            }
            int rc = bb.get() & 0xff;
            int next = rand.next() & 0xff;
            if (rc != next)
            {
                errors++;
            }
        }
        //System.err.println("received all");
        return errors;
    }
    
}
