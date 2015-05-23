/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vesalainen.comm.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;
import org.vesalainen.comm.channel.SerialChannel.Builder;
import org.vesalainen.comm.channel.SerialChannel.Configuration;
import org.vesalainen.comm.channel.SerialChannel.DataBits;
import org.vesalainen.comm.channel.SerialChannel.FlowControl;
import org.vesalainen.comm.channel.SerialChannel.Parity;
import org.vesalainen.comm.channel.SerialChannel.Speed;
import org.vesalainen.comm.channel.SerialChannel.StopBits;
import org.vesalainen.loader.LibraryLoader;
import org.vesalainen.loader.LibraryLoader.OS;
import static org.vesalainen.loader.LibraryLoader.OS.Linux;

/**
 * These test needs two serial ports connected together with null modem cable.
 * @author tkv
 */
public class LoopT
{
    static OS os = LibraryLoader.getOS();
    public LoopT()
    {
    }

    //@Test
    public void testSelectWrite()
    {
        if (os == Linux)
        {
            List<String> ports = SerialChannel.getFreePorts();
            assertNotNull(ports);
            assertTrue(ports.size() >= 2);
            try
            {
                Builder builder1 = new Builder(ports.get(0), Speed.B115200)
                        .setBlocking(false)
                        .setFlowControl(FlowControl.XONXOFF);
                Builder builder2 = new Builder(ports.get(1), Speed.B115200)
                        .setBlocking(false)
                        .setFlowControl(FlowControl.XONXOFF);
                try (SerialChannel c1 = builder1.get();
                    SerialChannel c2 = builder2.get()
                        )
                {
                    SerialSelector selector = new SerialSelector();
                    int size = 1000;
                    byte[] buf = new byte[size];
                    RandomChar rc = new RandomChar();
                    for (int ii=0;ii<size;ii++)
                    {
                        buf[ii] = (byte) rc.next();
                    }
                    selector.register(c1, OP_READ, ByteBuffer.allocate(size));
                    selector.register(c2, OP_READ, ByteBuffer.allocate(size));
                    selector.register(c1, OP_WRITE, ByteBuffer.wrap(buf));
                    selector.register(c2, OP_WRITE, ByteBuffer.wrap(buf));
                    while (selector.isOpen())
                    {
                        int kc = selector.select(5000);
                        if (kc > 0)
                        {
                            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                            while (iterator.hasNext())
                            {
                                SelectionKey sk = iterator.next();
                                if (sk.isReadable())
                                {
                                    ByteBuffer bb = (ByteBuffer) sk.attachment();
                                    SerialChannel channel = (SerialChannel) sk.channel();
                                    int rem1 = bb.remaining();
                                    int read = channel.read(bb);
                                    int rem2 = bb.remaining();
                                    assertEquals(rem1-rem2, read);
                                    if (!bb.hasRemaining())
                                    {
                                        sk.cancel();
                                        byte[] array = bb.array();
                                        if (!Arrays.equals(buf, array))
                                        {
                                            Arrays.toString(array);
                                        }
                                        assertTrue(Arrays.equals(buf, bb.array()));
                                    }
                                    //System.err.println("read "+bb);
                                }
                                if (sk.isWritable())
                                {
                                    ByteBuffer bb = (ByteBuffer) sk.attachment();
                                    SerialChannel channel = (SerialChannel) sk.channel();
                                    int limit = Math.min(bb.capacity(), bb.position()+size/5);
                                    bb.limit(limit);
                                    channel.write(bb);
                                    if (bb.position() == bb.capacity())
                                    {
                                        sk.cancel();
                                    }
                                    //System.err.println("write "+bb);
                                }
                                iterator.remove();
                            }
                        }
                        else
                        {
                            assertTrue(selector.keys().isEmpty());
                            selector.close();
                        }
                    }
                }
            }
            catch (IOException ex)
            {
                Logger.getLogger(LoopT.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    //@Test
    public void testReplaceError()
    {
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() >= 2);
        try
        {
            Builder builder1 = new Builder(ports.get(0), Speed.B1200)
                    .setReplaceError(true)
                    .setBlocking(false)
                    .setParity(Parity.EVEN);
            Builder builder2 = new Builder(ports.get(1), Speed.B1200)
                    .setReplaceError(true)
                    .setBlocking(false)
                    .setParity(Parity.SPACE);
            try (SerialChannel c1 = builder1.get();
                SerialChannel c2 = builder2.get()
                    )
            {
                SerialSelector selector = new SerialSelector();
                SelectionKey skr2 = selector.register(c2, OP_READ, null);
                ByteBuffer bb = ByteBuffer.allocateDirect(10);
                bb.put((byte)31);
                bb.flip();
                int rc = c1.write(bb);
                assertTrue("hangs", selector.select(1000) > 0);
                bb.clear();
                c2.read(bb);
                bb.flip();
                byte[] er = c2.getErrorReplacement();
                for (byte b : er)
                {
                    byte rb = bb.get();
                    assertEquals(b, rb);
                }
                if (os == OS.Linux)
                {
                    assertEquals((byte)31, bb.get());
                }
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(LoopT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //@Test
    public void testWakeupSelect()
    {
        final ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() >= 2);
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
    //@Test
    public void testSelect()
    {
        //SerialChannel.debug(true);
        final ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() >= 2);
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
                        try
                        {
                            ftra1.get();
                            ftra2.get();
                        }
                        catch (InterruptedException ex)
                        {
                            Logger.getLogger(LoopT.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        catch (ExecutionException ex)
                        {
                            fail(ex.getMessage());
                        }
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
                                    int next = rc.next();
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
    //@Test
    public void regressionTest()
    {
        //SerialChannel.debug(true);
        ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() >= 2);
        try 
        {
            for (FlowControl flow : new FlowControl[] {FlowControl.NONE})
            {
                for (Parity parity : new Parity[] {Parity.NONE, Parity.EVEN, Parity.SPACE})
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

    @Test
    public synchronized void autoConfTest() throws InterruptedException
    {
        //SerialChannel.debug(true);
        ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() >= 2);
        List<Configuration> randConfigs = new ArrayList<>();
        List<Configuration> optConfigs = new ArrayList<>();
        for (FlowControl flow : new FlowControl[] {FlowControl.NONE})
        {
            for (Parity parity : new Parity[] {Parity.NONE, Parity.EVEN, Parity.ODD, Parity.MARK, Parity.SPACE})
            {
                for (DataBits bits : new DataBits[] {DataBits.DATABITS_8})
                {
                    for (StopBits stops : new StopBits[] {StopBits.STOPBITS_10, StopBits.STOPBITS_20})
                    {
                        for (Speed speed : new Speed[] {Speed.B4800, Speed.B38400})
                        {
                            Configuration conf = new Configuration()
                            .setDataBits(bits)
                            .setFlowControl(flow)
                            .setParity(parity)
                            .setStopBits(stops)
                            .setSpeed(speed);
                            randConfigs.add(conf);
                            optConfigs.add(conf);
                        }
                    }
                }
            }
        }
        Random random = new Random(12345);
        Collections.shuffle(randConfigs, random);
        for (Configuration randConf : randConfigs)
        {
            System.err.println("\ntransmit "+randConf);
            wait(3000);
            Builder builder = new Builder(ports.get(0), randConf);
            try (SerialChannel sc = builder.get())
            {
                sc.setClearOnClose(true);
                Transmitter tra = new Transmitter(sc, Integer.MAX_VALUE, new RandomASCII());
                Future<Void> ftra = exec.submit(tra);
                AutoConfigurer ac = new AutoConfigurer(1000, 100, 1000);
                ac.addConfigurations(optConfigs);
                ac.addRange((byte)'\r');
                ac.addRange((byte)'\n');
                ac.addRange((byte)' ', (byte)0b1111111);
                Map<String, Configuration> map = ac.configure(ports.subList(1, ports.size()), 1, TimeUnit.DAYS);
                assertEquals(1, map.size());
                Configuration detectedConf = map.get(ports.get(1));
                assertEquals(randConf.speed, detectedConf.speed);
                comp(randConf, detectedConf);
                ftra.cancel(true);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                fail(ex.getMessage());
            }
        }
    }
    void comp(Configuration c1, Configuration c2)
    {
        if (c1.speed != c2.speed)
        {
            System.err.print(c1.speed+" != "+c2.speed+" ");
        }
        if (c1.dataBits != c2.dataBits)
        {
            System.err.print(c1.dataBits+" != "+c2.dataBits+" ");
        }
        if (c1.parity != c2.parity)
        {
            System.err.print(c1.parity+" != "+c2.parity+" ");
        }
        if (c1.stopBits != c2.stopBits)
        {
            System.err.print(c1.stopBits+" != "+c2.stopBits);
        }
        System.err.println();
    }
}
