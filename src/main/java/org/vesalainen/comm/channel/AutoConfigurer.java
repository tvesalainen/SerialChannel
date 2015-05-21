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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_READ;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.vesalainen.comm.channel.SerialChannel.Builder;
import org.vesalainen.comm.channel.SerialChannel.Configuration;

/**
 *
 * @author tkv
 */
public class AutoConfigurer
{
    private enum State {Success, Fail, GoOn };
    
    private List<Configuration> configurations = new ArrayList<>();
    /**
     * Adds configuration candidate
     * @param config 
     */
    public void addConfiguration(Configuration config)
    {
        configurations.add(config);
    }
    
    public Map<String, Configuration> configure(List<String> ports, long timeout, TimeUnit unit) throws IOException
    {
        if (configurations.isEmpty())
        {
            throw new IllegalArgumentException("no configurations");
        }
        long timeLimit = unit.toMillis(timeout)+System.currentTimeMillis();
        Map<String, Configuration> map = new HashMap<>();
        SerialSelector selector = new SerialSelector();
        try (CloseableSet<SerialChannel> channels = openAll(ports))
        {
            for (SerialChannel sc : channels)
            {
                selector.register(sc, OP_READ, new Ctx());
            }
            while (hasTimeLeft(timeLimit))
            {
                int count = selector.select(5000);
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
                                map.put(ctx.getPort(), ctx.getConfiguration());
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
        Matcher matcher = new Matcher(SerialChannel.getErrorReplacement());
        
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
                            confNo++;
                            if (confNo == configurations.size())
                            {
                                return s;
                            }
                            else
                            {
                                channel.configure(configurations.get(confNo));
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
                channel.configure(configurations.get(confNo));
                matcher.reset();
                return true;
            }
        }
        
    }
    private class Matcher
    {
        byte[] prefix;
        int index;
        int bytes;
        int errors;
        int skip = 20;

        public Matcher(byte[] prefix)
        {
            this.prefix = prefix;
        }
        
        State match(byte b)
        {
            if (skip-- < 0)
            {
                if (prefix[index++] == b)
                {
                    if (index == prefix.length)
                    {
                        errors++;
                        index = 0;
                    }
                }
                else
                {
                    bytes++;
                    index = 0;
                }
                if (bytes > 100)
                {
                    float ratio = (float)errors/(float)bytes;
                    reset();
                    errors = 0;
                    bytes = 0;
                    skip = 20;
                    if (ratio > 0.1)
                    {
                        return State.Fail;
                    }
                    else
                    {
                        return State.Success;
                    }
                }
            }
            return State.GoOn;
        }

        private void reset()
        {
            errors = 0;
            bytes = 0;
            skip = 20;
        }
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
