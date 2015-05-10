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
import java.util.List;
import java.util.concurrent.Callable;
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
import org.vesalainen.comm.channel.SerialChannel.DataBits;
import org.vesalainen.comm.channel.SerialChannel.FlowControl;
import org.vesalainen.comm.channel.SerialChannel.Parity;
import org.vesalainen.comm.channel.SerialChannel.Speed;

/**
 *
 * @author tkv
 */
public class SerialChannelT
{
    
    public SerialChannelT()
    {
    }

    //@Test
    public void test1()
    {
        try
        {
            List<String> allPorts = SerialChannel.getAllPorts();
            assertNotNull(allPorts);
            for (String port : allPorts)
            {
                Builder builder = new Builder(port, 4800)
                        .setParity(SerialChannel.Parity.SPACE);
                SerialChannel sc = builder.get();
                sc.connect();
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
    @Test
    public void testSelect()
    {
        List<String> ports = SerialChannel.getAllPorts();
        assertNotNull(ports);
        if (ports.size() >= 2)
        {
            try
            {
                SerialSelector selector = new SerialSelector();
                Builder builder1 = new Builder("COM17", Speed.CBR_1200)
                        .setBlocking(false);
                //Builder builder2 = new Builder(ports.get(1), Speed.CBR_1200)
                  //      .setBlocking(false);
                try (SerialChannel c1 = builder1.get();
                    //SerialChannel c2 = builder2.get()
                        )
                {
                    ByteBuffer bb = ByteBuffer.allocate(20);
                    SelectionKey sk1 = selector.register(c1, OP_READ, null);
                    while (true)
                    {
                        int cnt = selector.select();
                        if (cnt > 0)
                        {
                            for (SelectionKey sk : selector.selectedKeys())
                            {
                                if (sk.isReadable())
                                {
                                    c1.read(bb);
                                    bb.flip();
                                    while (bb.hasRemaining())
                                    {
                                        System.err.print((char)bb.get());
                                    }
                                    bb.clear();
                                }
                            }
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
    //@Test
    public void regressionTest()
    {
        ExecutorService exec = Executors.newCachedThreadPool();
        List<String> ports = SerialChannel.getAllPorts();
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
                            for (Speed speed : new Speed[] {Speed.CBR_4800, Speed.CBR_38400, Speed.CBR_256000})
                            {
                                System.err.println(speed+" "+bits+" "+parity+" "+flow);
                                int count = Integer.parseInt(speed.name().substring(4))/4;
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
    public class Transmitter implements Callable<Void>
    {
        private final SerialChannel channel;
        private final int count;

        public Transmitter(SerialChannel channel, int count)
        {
            this.channel = channel;
            this.count = count;
        }

        @Override
        public Void call() throws Exception
        {
            int bits = channel.getDataBits().ordinal()+4;
            RandomChar rand = new RandomChar();
            try (OutputStream os = channel.getOutputStream(70))
            {
                for (int ii=0;ii<count;ii++)
                {
                    int next = rand.next(bits);
                    os.write(next);
                }
            }
            return null;
        }
    }

    public class Receiver implements Callable<Integer>
    {
        private final SerialChannel channel;
        private final int count;

        public Receiver(SerialChannel channel, int count)
        {
            this.channel = channel;
            this.count = count;
        }

        @Override
        public Integer call() throws Exception
        {
            int errors = 0;
            int bits = channel.getDataBits().ordinal()+4;
            RandomChar rand = new RandomChar();
            try (InputStream is = channel.getInputStream(100))
            {
                for (int ii=0;ii<count;ii++)
                {
                    int rc = is.read();
                    int next = rand.next(bits);
                    if (rc != next)
                    {
                        errors++;
                    }
                }
            }
            return errors;
        }
        
    }
    
}
