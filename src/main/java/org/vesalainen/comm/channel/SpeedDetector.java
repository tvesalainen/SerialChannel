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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.vesalainen.comm.channel.SerialChannel.Builder;
import org.vesalainen.comm.channel.SerialChannel.Configuration;
import org.vesalainen.comm.channel.SerialChannel.Speed;

/**
 * This class is experimental!!!
 * @author tkv
 */
public class SpeedDetector
{
    private enum State {Success, Fail, GoOn };
    
    private long waitMillis;
    private int testLength;
    private int maxCount;
    private List<Range> ranges = new ArrayList<>();
    
    private List<Configuration> configurations = new ArrayList<>();

    public SpeedDetector()
    {
        this(1000, 20, 100);
    }
    public SpeedDetector(long waitMillis, int testLength, int maxCount)
    {
        this.waitMillis = waitMillis;
        this.testLength = testLength;
        this.maxCount = maxCount;
    }
    public void addRange(byte b)
    {
        ranges.add(new Range(b));
    }
    public void addRange(byte start, byte end)
    {
        ranges.add(new Range(start, end));
    }
    /**
     * Adds configuration candidate
     * @param config 
     */
    public void addSpeed(Speed speed)
    {
        configurations.add(new Configuration().setSpeed(speed));
    }
    public void addSpeeds(Collection<? extends Speed> all)
    {
        for (Speed speed : all)
        {
            addSpeed(speed);
        }
    }
    public Map<String, Speed> configure(List<String> ports, long timeout, TimeUnit unit) throws IOException
    {
        if (configurations.isEmpty())
        {
            throw new IllegalArgumentException("no configurations");
        }
        if (ranges.isEmpty())
        {
            throw new IllegalArgumentException("no ranges");
        }
        long timeLimit = unit.toMillis(timeout)+System.currentTimeMillis();
        Map<String, Speed> map = new HashMap<>();
        SerialSelector selector = new SerialSelector();
        System.err.println("try "+configurations.get(0));
        try (CloseableSet<SerialChannel> channels = openAll(ports))
        {
            for (SerialChannel sc : channels)
            {
                selector.register(sc, OP_READ, new Ctx(sc.getPort()));
            }
            while (hasTimeLeft(timeLimit))
            {
                int count = selector.select(waitMillis);
                if (count > 0)
                {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext())
                    {
                        SelectionKey sk = iterator.next();
                        Ctx ctx = (Ctx) sk.attachment();
                        switch (ctx.tryMatch(sk))
                        {
                            case Success:
                                map.put(ctx.getPort(), ctx.getConfiguration().getSpeed());
                                sk.cancel();
                                break;
                            case Fail:
                                sk.cancel();
                                break;
                        }
                        iterator.remove();
                    }
                }
                else
                {
                    // timeout
                    if (selector.keys().isEmpty())
                    {
                        break;
                    }
                    else
                    {
                        for (SelectionKey sk : selector.keys())
                        {
                            Ctx ctx = (Ctx) sk.attachment();
                            boolean has = ctx.nextConfiguration(sk);
                            if (!has)
                            {
                                sk.cancel();
                            }
                        }
                    }
                }
            }
        }
        return map;
    }
    private static boolean hasTimeLeft(long timeLimit)
    {
        return timeLeft(timeLimit) > 10;
    }
    private static long timeLeft(long timeLimit)
    {
        return timeLimit - System.currentTimeMillis();
    }
    
    private CloseableSet<SerialChannel> openAll(List<String> ports) throws IOException
    {
        CloseableSet<SerialChannel> set = new CloseableSet<>();
        Builder builder = new Builder("", configurations.get(0))
                .setBlocking(false)
                .setReplaceError(true);
        for (String port : ports)
        {
            builder.setPort(port);
            set.add(builder.get());
        }
        return set;
    }
    private class Ctx
    {
        String port;
        int confNo;
        ByteBuffer bb = ByteBuffer.allocate(1000);
        Matcher matcher = new Matcher(ranges);

        public Ctx(String port)
        {
            this.port = port;
        }
        
        State tryMatch(SelectionKey sk) throws IOException
        {
            SerialChannel channel = (SerialChannel) sk.channel();
            bb.clear();
            int rc = channel.read(bb);
            while (rc > 0)
            {
                bb.flip();
                while (bb.hasRemaining())
                {
                    State s = matcher.match(bb.get());
                    switch (s)
                    {
                        case Success:
                            return s;
                        case Fail:
                            if (!nextConfiguration(sk))
                            {
                                return s;
                            }
                            break;
                    }
                }
                bb.clear();
                rc = channel.read(bb);
            }
            return State.GoOn;
        }
        Configuration getConfiguration()
        {
            return configurations.get(confNo);
        }

        public String getPort()
        {
            return port;
        }

        private boolean nextConfiguration(SelectionKey sk) throws IOException
        {
            confNo++;
            if (confNo == configurations.size())
            {
                return false;
            }
            else
            {
                SerialChannel channel = (SerialChannel) sk.channel();
                Configuration conf = configurations.get(confNo);
                System.err.println("\ntry "+conf);
                channel.configure(conf);
                matcher.reset();
                return true;
            }
        }
        
    }
    private class Matcher
    {
        int corrects;
        int errors;
        int count;
        private final List<Range> ranges;

        public Matcher(List<Range> ranges)
        {
            this.ranges = ranges;
        }
        
        State match(byte b)
        {
            if (count > maxCount)
            {
                return State.Fail;
            }
            count++;
            if (!inRange(ranges, b))
            {
                errors++;
                corrects = 0;
                System.err.print(b+"-");
            }
            else
            {
                errors = 0;
                corrects++;
                System.err.print('+');
            }
            if (corrects > testLength)
            {
                return State.Success;
            }
            if (errors > testLength)
            {
                return State.Fail;
            }
            return State.GoOn;
        }

        private void reset()
        {
            errors = 0;
            corrects = 0;
            count = 0;
        }
    }
    public class Range
    {
        byte start;
        byte end;

        public Range(byte b)
        {
            this.start = b;
            this.end = b;
        }

        public Range(byte start, byte end)
        {
            this.start = start;
            this.end = end;
        }
        boolean in(byte b)
        {
            return b >= start && b <= end;
        }
    }
    static boolean inRange(Collection<? extends Range> ranges, byte b)
    {
        for (Range r : ranges)
        {
            if (r.in(b))
            {
                return true;
            }
        }
        return false;
    }
    private class CloseableSet<T extends AutoCloseable> extends HashSet<T> implements AutoCloseable
    {

        @Override
        public void close() throws IOException
        {
            for (T item : this)
            {
                try
                {
                    item.close();
                }
                catch (Exception ex)
                {
                    if (ex instanceof IOException)
                    {
                        
                        throw (IOException)ex;
                    }
                    else
                    {
                        throw new IOException(ex);
                    }
                }
            }
        }
        
    }
}
