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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;
import org.vesalainen.comm.channel.SerialChannel.Configuration;

/**
 *
 * @author tkv
 */
public class AutoConfigurerT
{
    
    public AutoConfigurerT()
    {
    }

    @Test
    public void test1()
    {
        try
        {
            List<String> ports = SerialChannel.getFreePorts();
            assertNotNull(ports);
            assertTrue(ports.size() >= 1);
            AutoConfigurer ac = new AutoConfigurer();
            ac.addConfiguration(new Configuration()
                    .setSpeed(SerialChannel.Speed.B4800)
            );
            ac.addConfiguration(new Configuration()
                    .setSpeed(SerialChannel.Speed.B57600)
            );
            Map<String, Configuration> map = ac.configure(ports, 1, TimeUnit.DAYS);
            assertEquals(1, map.size());
        }
        catch (IOException ex)
        {
            fail(ex.getMessage());
        }
    }
    
}
