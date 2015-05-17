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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
        final ExecutorService exec = Executors.newCachedThreadPool();
        Timer timer = new Timer();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() > 0);
        if (ports.size() >= 1)
        {
            try 
            {
                ByteBuffer rb = ByteBuffer.allocateDirect(2000);
                SerialChannel.Builder builder = new SerialChannel.Builder(ports.get(0), Speed.B1200)
                        .setBlocking(false);
                RandomChar rcr = new RandomChar();
                RandomChar rcw = new RandomChar();
                try (SerialChannel sc = builder.get())
                {
                    SerialSelector selector = new SerialSelector();
                    sc.configureBlocking(false);
                    sc.register(selector, OP_READ);
                    for (SerialChannel.FlowControl flow : new SerialChannel.FlowControl[] {SerialChannel.FlowControl.XONXOFF})
                    {
                        for (SerialChannel.Parity parity : new SerialChannel.Parity[] {SerialChannel.Parity.NONE, SerialChannel.Parity.EVEN, SerialChannel.Parity.ODD, SerialChannel.Parity.SPACE})
                        {
                            for (SerialChannel.DataBits bits : new SerialChannel.DataBits[] {SerialChannel.DataBits.DATABITS_8})
                            {
                                for (SerialChannel.Speed speed : new SerialChannel.Speed[] {SerialChannel.Speed.B4800, SerialChannel.Speed.B115200})
                                {
                                    System.err.println(speed+" "+bits+" "+parity+" "+flow);
                                    final int count = SerialChannel.getSpeed(speed)/4;
                                    builder.setSpeed(speed)
                                            .setFlowControl(flow)
                                            .setParity(parity)
                                            .setDataBits(bits);
                                    sc.configure(builder);
                                    final SyncTransmitter tra = new SyncTransmitter(sc, count);;
                                    TimerTask task = new TimerTask() {

                                        @Override
                                        public void run()
                                        {
                                            Future<Void> ftra1 = exec.submit(tra);
                                        }
                                    };
                                    timer.schedule(task, 5000);
                                    while (true)
                                    {
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
                                                rb.flip();
                                                System.err.println(rb);
                                                while (rb.hasRemaining())
                                                {
                                                    int cc = rb.get() & 0xff;
                                                    int next = rcr.next(8);
                                                    System.err.println("rc="+rcr.count()+" "+cc+" "+next);
                                                    assertEquals("count="+rcr.count(), next, cc);
                                                    assertTrue(rcr.count() <= count);
                                                }
                                                keyIterator.remove();
                                            }
                                        }
                                        if (rcr.count() == count-1)
                                        {
                                            tra.ack();
                                        }
                                        if (rcr.count() == count)
                                        {
                                            rcr.resetCount();
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
}
