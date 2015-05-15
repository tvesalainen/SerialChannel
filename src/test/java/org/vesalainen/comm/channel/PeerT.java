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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * These test needs two host connected together with null modem cable.
 * @author tkv
 */
public class PeerT
{
    @Test
    public void regressionTest()
    {
        //SerialChannel.debug(true);
        ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() > 0);
        if (ports.size() >= 1)
        {
            try 
            {
                for (SerialChannel.FlowControl flow : new SerialChannel.FlowControl[] {SerialChannel.FlowControl.NONE})
                {
                    for (SerialChannel.Parity parity : new SerialChannel.Parity[] {SerialChannel.Parity.NONE})
                    {
                        for (SerialChannel.DataBits bits : new SerialChannel.DataBits[] {SerialChannel.DataBits.DATABITS_8})
                        {
                            for (SerialChannel.Speed speed : new SerialChannel.Speed[] {SerialChannel.Speed.B4800})
                            {
                                System.err.println(speed+" "+bits+" "+parity+" "+flow);
                                int count = SerialChannel.getSpeed(speed)/4;
                                SerialChannel.Builder builder1 = new SerialChannel.Builder(ports.get(0), speed)
                                        .setFlowControl(flow)
                                        .setParity(parity)
                                        .setDataBits(bits);
                                try (SerialChannel c1 = builder1.get())
                                {
                                    Receiver rec1 = new Receiver(c1, count);
                                    Future<Integer> frec1 = exec.submit(rec1);
                                    Transmitter tra1 = new Transmitter(c1, count);
                                    Future<Void> ftra1 = exec.submit(tra1);
                                    ftra1.get();
                                    assertEquals(Integer.valueOf(0), frec1.get(200, TimeUnit.SECONDS));
                                }
                                Thread.currentThread().wait(1000);
                            }
                        }
                    }
                }
            }
            catch (InterruptedException | ExecutionException | TimeoutException | IOException ex)
            {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        }
    }

    
}
