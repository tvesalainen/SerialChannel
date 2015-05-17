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
                SerialChannel.Builder builder = new SerialChannel.Builder(ports.get(0), Speed.B1200)
                        .setBlocking(false);
                try (SerialChannel sc = builder.get())
                {
                    for (SerialChannel.FlowControl flow : new SerialChannel.FlowControl[] {SerialChannel.FlowControl.XONXOFF})
                    {
                        for (SerialChannel.Parity parity : new SerialChannel.Parity[] {SerialChannel.Parity.NONE})
                        {
                            for (SerialChannel.DataBits bits : new SerialChannel.DataBits[] {SerialChannel.DataBits.DATABITS_8})
                            {
                                for (SerialChannel.Speed speed : new SerialChannel.Speed[] {SerialChannel.Speed.B4800, SerialChannel.Speed.B115200})
                                {
                                    System.err.println(speed+" "+bits+" "+parity+" "+flow);
                                    int count = SerialChannel.getSpeed(speed)/4;
                                    builder.setSpeed(speed)
                                            .setFlowControl(flow)
                                            .setParity(parity)
                                            .setDataBits(bits);
                                    sc.configure(builder);
                                    SerialSelector selector = new SerialSelector();
                                    sc.configureBlocking(false);
                                    sc.register(selector, OP_READ);
                                    RandomChar rcr = new RandomChar();
                                    RandomChar rcw = new RandomChar();
                                    send(sc, bb, rcw, count);
                                    int c = selector.select();
                                    if (c > 0)
                                    {
                                        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                                        while(keyIterator.hasNext())
                                        {
                                            SelectionKey sk = keyIterator.next();
                                            if (sk.isReadable())
                                            {
                                                bb.clear();
                                                sc.read(bb);
                                                bb.flip();
                                                System.err.println(bb);
                                                while (bb.hasRemaining())
                                                {
                                                    int cc = bb.get() & 0xff;
                                                    int next = rcr.next(8);
                                                    System.err.println(cc+" "+next);
                                                    assertEquals("count="+rcr.count(), (byte)next, cc);
                                                    assertTrue(rcr.count() <= count);
                                                }
                                                if (rcr.count() == count)
                                                {
                                                    rcr.resetCount();
                                                    sk.cancel();
                                                }
                                                keyIterator.remove();
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
            catch (IOException ex)
            {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        }
    }

    private void send(SerialChannel sc, ByteBuffer bb, RandomChar rcw, int count) throws IOException
    {
        System.err.println("send");
        bb.clear();
        while (rcw.count() < count && bb.hasRemaining())
        {
            bb.put((byte) rcw.next(8));
        }
        bb.flip();
        sc.write(bb);
    }

    
}
