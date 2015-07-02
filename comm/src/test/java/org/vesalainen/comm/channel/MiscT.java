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
import java.io.InputStream;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author tkv
 */
public class MiscT
{
    @Test
    public void test1()
    {
        try
        {
            List<String> allPorts = SerialChannel.getFreePorts();
            assertNotNull(allPorts);
            for (String port : allPorts)
            {
                SerialChannel.Builder builder = new SerialChannel.Builder(port, SerialChannel.Speed.B4800)
                        .setParity(SerialChannel.Parity.SPACE)
                        .setReplaceError(true);
                SerialChannel sc = builder.get();
                try (InputStream is = sc.getInputStream(80))
                {
                    int cc = is.read();
                    while (cc != -1)
                    {
                        System.err.println(String.format("%02X ", cc));
                        cc = is.read();
                    }
                }
            }
        }
        catch (IOException ex)
        {
            fail(ex.getMessage());
        }
    }
    
}
