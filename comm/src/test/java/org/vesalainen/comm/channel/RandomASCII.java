/*
 * Copyright (C) 2015 Timo Vesalainen <timo.vesalainen@iki.fi>
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

/**
 *
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class RandomASCII extends RandomChar
{
    int bound;
    
    public RandomASCII()
    {
        super(7);
        bound = 1<<(7);
    }

    @Override
    public int next()
    {
        int r = random.nextInt(bound);
        if (r >= bound)
        {
            System.err.println();
        }
        if (r < 32)
        {
            return 32;
        }
        return r;
    }
    
}
