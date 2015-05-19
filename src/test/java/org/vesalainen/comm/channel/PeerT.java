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
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.vesalainen.comm.channel.SerialChannel.Speed;

/**
 * These test needs two host connected together with a null modem cable.
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
            try (SimpleSync ss = SimpleSync.open(12345))
            {
                ByteBuffer wb = ByteBuffer.allocateDirect(10);
                ByteBuffer rb = ByteBuffer.allocateDirect(100);
                SerialChannel.Builder builder = new SerialChannel.Builder(ports.get(0), Speed.B1200)
                        .setBlocking(false);
                RandomChar rcr = new RandomChar();
                RandomChar rcw = new RandomChar();
                for (SerialChannel.FlowControl flow : new SerialChannel.FlowControl[] {SerialChannel.FlowControl.NONE})
                {
                    for (SerialChannel.Parity parity : new SerialChannel.Parity[] {SerialChannel.Parity.NONE, SerialChannel.Parity.EVEN, SerialChannel.Parity.ODD, SerialChannel.Parity.SPACE})
                    {
                        for (SerialChannel.DataBits bits : new SerialChannel.DataBits[] {SerialChannel.DataBits.DATABITS_8})
                        {
                            for (SerialChannel.Speed speed : new SerialChannel.Speed[] {SerialChannel.Speed.B4800, SerialChannel.Speed.B38400})
                            {
                                System.err.println(speed+" "+bits+" "+parity+" "+flow);
                                final int count = 256;
                                builder.setSpeed(speed)
                                        .setFlowControl(flow)
                                        .setParity(parity)
                                        .setDataBits(bits);
                                ss.sync();
                                try (SerialChannel sc = builder.get())
                                {
                                    sc.configureBlocking(false);
                                    SerialSelector selector = new SerialSelector();
                                    sc.register(selector, OP_READ);
                                    System.err.println("wait");
                                    ss.sync();
                                    while (selector.isOpen())
                                    {
                                        send(sc, wb, rcw, count);
                                        int c = selector.select();
                                        assertTrue(c > 0);
                                        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                                        while(keyIterator.hasNext())
                                        {
                                            SelectionKey sk = keyIterator.next();
                                            if (sk.isReadable())
                                            {
                                                rb.clear();
                                                sc.read(rb);
                                                //if (rb.position() == rb.capacity())
                                                {
                                                    System.err.println(rb);
                                                }
                                                rb.flip();
                                                //System.err.println(rb);
                                                while (rb.hasRemaining())
                                                {
                                                    int cc = rb.get() & 0xff;
                                                    int next = rcr.next(8);
                                                    //System.err.println("rc="+rcr.count()+" "+cc+" "+next);
                                                    if (next != cc)
                                                    {
                                                        System.err.println();
                                                    }
                                                    assertEquals("count="+rcr.count(), next, cc);
                                                    assertTrue(rcr.count() <= count);
                                                }
                                                keyIterator.remove();
                                            }
                                        }
                                        if (rcr.count() == count)
                                        {
                                            rcr.resetCount();
                                            rcw.resetCount();
                                            selector.close();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (IOException ex)
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
