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
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import org.vesalainen.comm.channel.SerialChannel.Builder;
import org.vesalainen.comm.channel.SerialChannel.Configuration;
import org.vesalainen.comm.channel.SerialChannel.DataBits;
import org.vesalainen.comm.channel.SerialChannel.FlowControl;
import org.vesalainen.comm.channel.SerialChannel.Parity;
import org.vesalainen.comm.channel.SerialChannel.Speed;
import org.vesalainen.loader.LibraryLoader;
import org.vesalainen.loader.LibraryLoader.OS;
import static org.vesalainen.loader.LibraryLoader.OS.Linux;
import org.vesalainen.util.CollectionHelp;

/**
 * These test needs two serial ports connected together with null modem cable.
 * @author Timo Vesalainen <timo.vesalainen@iki.fi>
 */
public class LoopT
{
    static OS os = LibraryLoader.getOS();
    ExecutorService exec = Executors.newCachedThreadPool();
    public LoopT()
    {
        //SerialChannel.debug(true);
    }

    @After
    public synchronized void after() throws InterruptedException
    {
        wait(5000);
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
                Builder builder1 = new Builder(ports.get(0), Speed.B1200)
                        .setBlocking(false)
                        .setFlowControl(FlowControl.XONXOFF);
                Builder builder2 = new Builder(ports.get(1), Speed.B1200)
                        .setBlocking(false)
                        .setFlowControl(FlowControl.XONXOFF);
                try (SerialChannel c1 = builder1.get();
                    SerialChannel c2 = builder2.get()
                        )
                {
                    SerialSelector selector = SerialSelector.open();
                    int size = 1000;
                    byte[] buf = new byte[size];
                    RandomChar rc = new RandomChar();
                    for (int ii=0;ii<size;ii++)
                    {
                        buf[ii] = (byte) rc.next();
                    }
                    Random rand = new Random(123456);
                    c1.register(selector, OP_READ, alloc(size, rand));
                    c2.register(selector, OP_READ, alloc(size, rand));
                    c1.register(selector, OP_WRITE, alloc(buf, rand));
                    c2.register(selector, OP_WRITE, alloc(buf, rand));
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
                                    ByteBuffer[] bba = (ByteBuffer[]) sk.attachment();
                                    SerialChannel channel = (SerialChannel) sk.channel();
                                    int rem1 = remaining(bba);
                                    int off = offset(bba);
                                    long read = channel.read(bba, off, bba.length-off);
                                    System.err.println("read "+read);
                                    int rem2 = remaining(bba);
                                    assertEquals(rem1-rem2, read);
                                    if (remaining(bba) == 0)
                                    {
                                        sk.cancel();
                                        assertTrue(equals(buf, bba));
                                    }
                                    //System.err.println("read "+bb);
                                }
                                if (sk.isWritable())
                                {
                                    ByteBuffer[] bba = (ByteBuffer[]) sk.attachment();
                                    SerialChannel channel = (SerialChannel) sk.channel();
                                    int off = offset(bba);
                                    long write = channel.write(bba, off, bba.length-off);
                                    System.err.println("write "+write);
                                    if (remaining(bba) == 0)
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
                fail(ex.getMessage());
                Logger.getLogger(LoopT.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private int offset(ByteBuffer[] bba)
    {
        int off = 0;
        for (ByteBuffer bb : bba)
        {
            if (bb.hasRemaining())
            {
                return off;
            }
            off++;
        }
        return off;
    }
    private int remaining(ByteBuffer[] bba)
    {
        int remaining = 0;
        for (ByteBuffer bb : bba)
        {
            remaining += bb.remaining();
        }
        return remaining;
    }
    private boolean equals(byte[] buf, ByteBuffer[] bba)
    {
        int off=0;
        for (ByteBuffer bb : bba)
        {
            bb.clear();
            while (bb.hasRemaining())
            {
                byte b = bb.get();
                if (b != buf[off])
                {
                    System.err.println("error at "+off);
                    return false;
                }
                off++;
            }
        }
        return true;
    }
    private ByteBuffer[] alloc(byte[] buf, Random rand)
    {
        int off=0;
        ByteBuffer[] bba = alloc(buf.length, rand);
        for (ByteBuffer bb : bba)
        {
            int remaining = bb.remaining();
            bb.put(buf, off, remaining);
            bb.flip();
            off += remaining;
        }
        return bba;
    }
    private ByteBuffer[] alloc(int size, Random rand)
    {
        boolean direct = false;
        List<ByteBuffer> list = new ArrayList<>();
        int limit = size/3;
        while (size > limit)
        {
            int next = rand.nextInt(size+1);
            if (direct)
            {
                list.add(ByteBuffer.allocateDirect(next));
            }
            else
            {
                list.add(ByteBuffer.allocate(next));
            }
            direct = !direct;
            size -= next;
        }
        list.add(ByteBuffer.allocateDirect(size));
        return list.toArray(new ByteBuffer[list.size()]);
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
                SerialSelector selector = SerialSelector.open();
                SelectionKey skr2 = c2.register(selector, OP_READ, null);
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
                assertEquals((byte)31, bb.get());
            }
        }
        catch (IOException ex)
        {
            fail(ex.getMessage());
            Logger.getLogger(LoopT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //@Test
    public void testWakeupSelect()
    {
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() >= 2);
        try
        {
            final SerialSelector selector = SerialSelector.open();
            Builder builder1 = new Builder(ports.get(0), Speed.B1200)
                    .setBlocking(false);
            Builder builder2 = new Builder(ports.get(1), Speed.B1200)
                    .setBlocking(false);
            try (SerialChannel c1 = builder1.get();
                SerialChannel c2 = builder2.get()
                    )
            {
                SelectionKey skr1 = c1.register(selector, OP_READ, null);
                SelectionKey skr2 = c2.register(selector, OP_READ, null);
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
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() >= 2);
        try
        {
            SerialSelector selector = SerialSelector.open();
            Builder builder1 = new Builder(ports.get(0), Speed.B1200)
                    .setBlocking(false);
            Builder builder2 = new Builder(ports.get(1), Speed.B1200)
                    .setBlocking(false);
            try (SerialChannel c1 = builder1.get();
                SerialChannel c2 = builder2.get()
                    )
            {
                final int count = 1000;
                SelectionKey skr1 = c1.register(selector, OP_READ, new Object[] {c1, new RandomChar(), ByteBuffer.allocateDirect(101), count});
                SelectionKey skr2 = c2.register(selector, OP_READ, new Object[] {c2, new RandomChar(), ByteBuffer.allocateDirect(102), count});
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
    @Test
    public void regressionTest1() throws InterruptedException
    {
        List<String> ports = CollectionHelp.create("COM27", "COM28");    //SerialChannel.getFreePorts();
        List<String> ports2 = CollectionHelp.create("COM29", "COM30");    //SerialChannel.getFreePorts();
        exec.submit(()->regressionTest(ports));
        exec.submit(()->regressionTest(ports2));
        exec.awaitTermination(1, TimeUnit.MINUTES);
    }
    public void regressionTest(List<String> ports)
    {
        System.err.println("regressionTest "+ports);
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
                            System.gc();
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

    //@Test
    public synchronized void autoConfTest() throws InterruptedException
    {
        List<String> ports = SerialChannel.getFreePorts();
        assertNotNull(ports);
        assertTrue(ports.size() >= 2);
        Speed speed = Speed.B38400;
        System.err.println("\ntransmit "+speed);
        wait(3000);
        Builder builder = new Builder(ports.get(0), Speed.B4800);
        try (SerialChannel sc = builder.get())
        {
            sc.setClearOnClose(true);
            Transmitter tra = new Transmitter(sc, Integer.MAX_VALUE, new RandomASCII());
            Future<Void> ftra = exec.submit(tra);
            SpeedDetector sd = new SpeedDetector(1000, 100, 1000);
            sd.addSpeed(Speed.B1200);
            sd.addSpeed(Speed.B4800);
            sd.addSpeed(Speed.B38400);
            sd.addRange((byte)'\r');
            sd.addRange((byte)'\n');
            sd.addRange((byte)' ', (byte)0b1111111);
            Map<String, Speed> map = sd.configure(ports.subList(1, ports.size()), 1, TimeUnit.DAYS);
            assertEquals(1, map.size());
            Speed detectedSpeed = map.get(ports.get(1));
            assertEquals(speed, detectedSpeed);
            ftra.cancel(true);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            fail(ex.getMessage());
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
