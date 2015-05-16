/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vesalainen.comm.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.vesalainen.comm.channel.SerialChannel.Builder;
import org.vesalainen.comm.channel.SerialChannel.DataBits;
import org.vesalainen.comm.channel.SerialChannel.FlowControl;
import org.vesalainen.comm.channel.SerialChannel.Parity;
import org.vesalainen.comm.channel.SerialChannel.Speed;

/**
 * These test needs two serial ports connected together with null modem cable.
 * @author tkv
 */
public class LoopT
{
    
    public LoopT()
    {
    }

    @Test
    public void test1()
    {
        try
        {
            List<String> allPorts = SerialChannel.getFreePorts();
            assertNotNull(allPorts);
            for (String port : allPorts)
            {
                Builder builder = new Builder(port, Speed.B57600)
                        .setParity(SerialChannel.Parity.SPACE);
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
    //@Test
    public void testWakeupSelect()
    {
        final ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        if (ports.size() >= 2)
        {
            try
            {
                final SerialSelector selector = new SerialSelector();
                Builder builder1 = new Builder(ports.get(0), Speed.B1200)
                        .setBlocking(false);
                Builder builder2 = new Builder(ports.get(1), Speed.B1200)
                        .setBlocking(false);
                try (SerialChannel c1 = builder1.get();
                    SerialChannel c2 = builder2.get()
                        )
                {
                    SelectionKey skr1 = selector.register(c1, OP_READ, null);
                    SelectionKey skr2 = selector.register(c2, OP_READ, null);
                    TimerTask task = new TimerTask() {

                        @Override
                        public void run()
                        {
                            selector.wakeup();
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(task, 1000);
                    long start = System.currentTimeMillis();
                    selector.select();
                    assertEquals(1000, System.currentTimeMillis()-start, 100);
                    selector.wakeup();
                    selector.select();
                    start = System.currentTimeMillis();
                    selector.select(1000);
                    assertEquals(1000, System.currentTimeMillis()-start, 100);
                }
            }
            catch (IOException ex)
            {
                fail(ex.getMessage());
            }
        }
    }
    //@Test
    public void testSelect()
    {
        final ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        if (ports.size() >= 2)
        {
            try
            {
                SerialSelector selector = new SerialSelector();
                Builder builder1 = new Builder(ports.get(0), Speed.B1200)
                        .setBlocking(false);
                Builder builder2 = new Builder(ports.get(1), Speed.B1200)
                        .setBlocking(false);
                try (SerialChannel c1 = builder1.get();
                    SerialChannel c2 = builder2.get()
                        )
                {
                    final int count = 1000;
                    SelectionKey skr1 = selector.register(c1, OP_READ, new Object[] {c1, new RandomChar(), ByteBuffer.allocateDirect(101), count});
                    SelectionKey skr2 = selector.register(c2, OP_READ, new Object[] {c2, new RandomChar(), ByteBuffer.allocateDirect(102), count});
                    TimerTask task = new TimerTask() {

                        @Override
                        public void run()
                        {
                            Transmitter tra1 = new Transmitter(c1, count);
                            Future<Void> ftra1 = exec.submit(tra1);
                            Transmitter tra2 = new Transmitter(c2, count);
                            Future<Void> ftra2 = exec.submit(tra2);
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(task, 1000);
                    while (selector.isOpen())
                    {
                        int cnt = selector.select();
                        if (cnt > 0)
                        {
                            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                            while(keyIterator.hasNext())
                            {
                                SelectionKey sk = keyIterator.next();
                                if (sk.isReadable())
                                {
                                    Object[] arr = (Object[]) sk.attachment();
                                    SerialChannel sc = (SerialChannel) arr[0];
                                    RandomChar rc = (RandomChar) arr[1];
                                    ByteBuffer bb = (ByteBuffer) arr[2];
                                    int c = (int) arr[3];
                                    bb.clear();
                                    sc.read(bb);
                                    bb.flip();
                                    while (bb.hasRemaining())
                                    {
                                        int next = rc.next(8);
                                        byte cc = bb.get();
                                        assertEquals((byte)next, cc);
                                        c--;
                                    }
                                    assertTrue(c >= 0);
                                    if (c == 0)
                                    {
                                        sk.cancel();
                                    }
                                    arr[3] = c;
                                }
                                keyIterator.remove();
                            }
                        }
                        else
                        {
                            selector.close();
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
    //@Test
    public void regressionTest()
    {
        //SerialChannel.debug(true);
        ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        if (ports.size() >= 2)
        {
            try 
            {
                for (FlowControl flow : new FlowControl[] {FlowControl.NONE})
                {
                    for (Parity parity : new Parity[] {Parity.NONE, Parity.SPACE})
                    {
                        for (DataBits bits : new DataBits[] {DataBits.DATABITS_8})
                        {
                            for (Speed speed : new Speed[] {Speed.B4800, Speed.B38400, Speed.B115200})
                            {
                                System.err.println(speed+" "+bits+" "+parity+" "+flow);
                                int count = SerialChannel.getSpeed(speed)/4;
                                Builder builder1 = new Builder(ports.get(0), speed)
                                        .setFlowControl(flow)
                                        .setParity(parity)
                                        .setDataBits(bits);
                                Builder builder2 = new Builder(ports.get(1), speed)
                                        .setFlowControl(flow)
                                        .setParity(parity)
                                        .setDataBits(bits);
                                try (SerialChannel c1 = builder1.get();
                                    SerialChannel c2 = builder2.get())
                                {
                                    Receiver rec1 = new Receiver(c1, count);
                                    Future<Integer> frec1 = exec.submit(rec1);
                                    Receiver rec2 = new Receiver(c2, count);
                                    Future<Integer> frec2 = exec.submit(rec2);
                                    Transmitter tra1 = new Transmitter(c1, count);
                                    Future<Void> ftra1 = exec.submit(tra1);
                                    Transmitter tra2 = new Transmitter(c2, count);
                                    Future<Void> ftra2 = exec.submit(tra2);
                                    assertEquals(Integer.valueOf(0), frec1.get(200, TimeUnit.SECONDS));
                                    assertEquals(Integer.valueOf(0), frec2.get(200, TimeUnit.SECONDS));
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

    
}
