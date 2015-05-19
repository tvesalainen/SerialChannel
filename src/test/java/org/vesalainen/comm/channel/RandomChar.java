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

import java.util.Random;

/**
 * Utility that creates pseudo random characters. The seed is always the same. Therefore
 * the character sequence is also the same. This is used in SerialChannel testing.
 * @author tkv
 */
public class RandomChar
{
    private static final int SEED = 123456;
    private int count;

    private Random random = new Random(SEED);
    public int next(int bits)
    {
        return (count++ % 200)+32;
        /*
        if (bits >= 7)
        {
            return random.nextInt((int)Math.pow(2, bits)-32)+32;
        }
        else
        {
            return random.nextInt((int)Math.pow(2, bits));
        }
                */
    }
    public int count()
    {
        return count;
    }
    public void resetCount()
    {
        count = 0;
    }
    public void reset()
    {
        random.setSeed(SEED);
    }

    @Override
    public String toString()
    {
        return "count=" + count;
    }
    
}
