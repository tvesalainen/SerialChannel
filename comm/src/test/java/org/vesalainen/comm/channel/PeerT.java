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
import org.vesalainen.util.CollectionHelp;

/**
 * These test needs two hosts connected together with a null modem cable.
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class PeerT
{
    @Test
    public void regressionTest()
    {
        //SerialChannel.debug(true);
        List<String> ports = CollectionHelp.create("COM27", "COM28");    //SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue("no ports", ports.size() > 0);
        if (ports.size() >= 1)
        {
            try (SimpleSync ss = SimpleSync.open(12345))
            {
                SerialChannel.Builder builder = new SerialChannel.Builder(ports.get(0), Speed.B1200)
                        .setBlocking(false);
                RandomChar rcr = new RandomChar();
                RandomChar rcw = new RandomChar();
                for (SerialChannel.FlowControl flow : new SerialChannel.FlowControl[] {SerialChannel.FlowControl.XONXOFF})
                {
                    for (SerialChannel.Parity parity : new SerialChannel.Parity[] {SerialChannel.Parity.NONE, SerialChannel.Parity.EVEN, SerialChannel.Parity.ODD, SerialChannel.Parity.SPACE, SerialChannel.Parity.MARK})
                    {
                        for (SerialChannel.DataBits bits : new SerialChannel.DataBits[] {SerialChannel.DataBits.DATABITS_8})
                        {
                            for (SerialChannel.Speed speed : new SerialChannel.Speed[] {SerialChannel.Speed.B1200, SerialChannel.Speed.B115200})
                            {
                                for (SerialChannel.StopBits stops : new SerialChannel.StopBits[] {SerialChannel.StopBits.STOPBITS_1, SerialChannel.StopBits.STOPBITS_2})
                                {
                                    System.err.println("\n"+speed+" "+bits+" "+parity+" "+flow+" "+stops);
                                    builder.setSpeed(speed)
                                            .setReplaceError(true)
                                            .setFlowControl(flow)
                                            .setParity(parity)
                                            .setStopBits(stops)
                                            .setDataBits(bits);
                                    final int count = builder.getBytesPerSecond()*5;    // 5 second test
                                    ByteBuffer wb = ByteBuffer.allocateDirect(2048);
                                    ByteBuffer rb = ByteBuffer.allocateDirect(2048);
                                    ss.sync();
                                    try (SerialChannel sc = builder.get())
                                    {
                                        sc.configureBlocking(false);
                                        SerialSelector selector = SerialSelector.open();
                                        sc.register(selector, OP_READ);
                                        System.err.println("wait");
                                        ss.sync();
                                        while (selector.isOpen())
                                        {
                                            send(sc, wb, rcw, count);
                                            int c = selector.select(5000);
                                            assertTrue("count="+rcr.count()+" < "+count, c > 0);
                                            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                                            while(keyIterator.hasNext())
                                            {
                                                SelectionKey sk = keyIterator.next();
                                                if (sk.isReadable())
                                                {
                                                    rb.clear();
                                                    int rc = sc.read(rb);
                                                    while (rc > 0)
                                                    {
                                                        if (rb.position() == rb.capacity())
                                                        {
                                                            System.err.println(rb);
                                                        }
                                                        rb.flip();
                                                        //System.err.println(rb);
                                                        while (rb.hasRemaining())
                                                        {
                                                            byte b = rb.get();
                                                            int cc = b & 0xff;
                                                            int next = rcr.next();
                                                            //System.err.println("rc="+rcr.count()+" "+cc+" "+next);
                                                            if (next != cc)
                                                            {
                                                                System.err.println("expected="+next+" got="+cc);
                                                            }
                                                            assertEquals("count="+rcr.count(), next, cc);
                                                            assertTrue("count="+rcr.count(), rcr.count() <= count);
                                                        }
                                                        rb.clear();
                                                        rc = sc.read(rb);
                                                    }
                                                    keyIterator.remove();
                                                }
                                            }
                                            if (rcr.count() == count)
                                            {
                                                System.err.println("\n----------------");
                                                while (send(sc, wb, rcw, count));
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
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        }
    }

    private boolean send(SerialChannel sc, ByteBuffer bb, RandomChar rcw, int count) throws IOException
    {
        bb.clear();
        while (rcw.count() < count && bb.hasRemaining())
        {
            byte next = (byte) rcw.next();
            //System.err.print(Byte.toUnsignedInt(next)+" ");
            bb.put(next);
        }
        bb.flip();
        boolean hasRemaining = bb.hasRemaining();
        while (bb.hasRemaining())
        {
            int rc = sc.write(bb);
        }
        return hasRemaining;
    }

    
}
