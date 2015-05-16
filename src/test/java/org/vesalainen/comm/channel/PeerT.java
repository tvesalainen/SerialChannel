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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
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
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() > 0);
        if (ports.size() >= 1)
        {
            try 
            {
                ByteBuffer bb = ByteBuffer.allocateDirect(10);
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
                                try (SerialChannel sc = builder1.get())
                                {
                                    SerialSelector selector = new SerialSelector();
                                    sc.configureBlocking(false);
                                    sc.register(selector, OP_READ);
                                    RandomChar rcr = new RandomChar();
                                    RandomChar rcw = new RandomChar();
                                    send(sc, bb, rcw, count);
                                    int c = selector.select();
                                    if (c > 0)
                                    {
                                        for (SelectionKey sk : selector.selectedKeys())
                                        {
                                            if (sk.isReadable())
                                            {
                                                bb.clear();
                                                sc.read(bb);
                                                bb.flip();
                                                while (bb.hasRemaining())
                                                {
                                                    byte cc = bb.get();
                                                    int next = rcr.next(8);
                                                    assertEquals(next, cc);
                                                    assertTrue(rcr.count() <= count);
                                                }
                                                if (rcr.count() == count)
                                                {
                                                    sk.cancel();
                                                }
                                            }
                                        }
                                        send(sc, bb, rcw, count);
                                    }
                                    else
                                    {
                                        continue;
                                    }
                                }
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

    private void send(SerialChannel sc, ByteBuffer bb, RandomChar rcw, int count) throws IOException
    {
        bb.clear();
        while (rcw.count() < count && bb.hasRemaining())
        {
            bb.put((byte) rcw.next(8));
        }
        bb.flip();
        sc.write(bb);
    }

    
}
