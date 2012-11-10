/*
 * Copyright (C) 2011 Timo Vesalainen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.vesalainen.comm.channel.test;

import org.vesalainen.comm.channel.SerialChannel;
import org.vesalainen.comm.channel.SerialChannel.DataBits;
import org.vesalainen.comm.channel.SerialChannel.FlowControl;
import org.vesalainen.comm.channel.SerialChannel.Parity;
import org.vesalainen.comm.channel.SerialChannel.Speed;
import org.vesalainen.comm.channel.SerialChannel.StopBits;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 *
 * @author tkv
 */
public class TestEnumerator
{
    private String port;
    private List<Setup> list = new ArrayList<Setup>();
    private Iterator<Setup> iterator;

    public TestEnumerator(String port)
    {
        this.port = port;
        for (SerialChannel.Speed speed : SerialChannel.Speed.values())
        {
            for (SerialChannel.Parity parity : SerialChannel.Parity.values())
            {
                for (SerialChannel.DataBits databits : SerialChannel.DataBits.values())
                {
                    for (SerialChannel.StopBits stopBits : SerialChannel.StopBits.values())
                    {
                        for (SerialChannel.FlowControl flowControl : SerialChannel.FlowControl.values())
                        {
                            if (databits.ordinal() >= SerialChannel.DataBits.DATABITS_7.ordinal() || !flowControl.equals(SerialChannel.FlowControl.XONXOFF))
                            {
                                list.add(new Setup(speed, parity, databits, stopBits, flowControl));
                            }
                        }
                    }
                }
            }
        }
        Collections.shuffle(list, new Random(321));
        iterator = list.iterator();
    }

    public SerialChannel next()
    {
        while (iterator.hasNext())
        {
            Setup setup = iterator.next();
            try
            {
                SerialChannel sc = SerialChannel.getInstance(port, setup.getSpeed(), setup.getParity(), setup.getDatabits(), setup.getStopBits(), setup.getFlowControl());
                sc.connect();
                return sc;
            }
            catch (IOException ex)
            {
            }
        }
        return null;
    }

    public static int count()
    {
        return 8000;
    }

    private class Setup
    {
        SerialChannel.Speed speed;
        SerialChannel.Parity parity;
        SerialChannel.DataBits databits;
        SerialChannel.StopBits stopBits;
        SerialChannel.FlowControl flowControl;

        public Setup(Speed speed, Parity parity, DataBits databits, StopBits stopBits, FlowControl flowControl)
        {
            this.speed = speed;
            this.parity = parity;
            this.databits = databits;
            this.stopBits = stopBits;
            this.flowControl = flowControl;
        }

        public DataBits getDatabits()
        {
            return databits;
        }

        public FlowControl getFlowControl()
        {
            return flowControl;
        }

        public Parity getParity()
        {
            return parity;
        }

        public Speed getSpeed()
        {
            return speed;
        }

        public StopBits getStopBits()
        {
            return stopBits;
        }

        @Override
        public String toString()
        {
            return "Setup{" + "speed=" + speed + "parity=" + parity + "databits=" + databits + "stopBits=" + stopBits + "flowControl=" + flowControl + '}';
        }

    }
}
