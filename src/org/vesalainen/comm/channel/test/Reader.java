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

/**
 * This utility opens a SerialChannel. It reads pseudo random characters and check
 * if they are in the same sequence.
 */
package org.vesalainen.comm.channel.test;

import org.vesalainen.comm.channel.CommEvent.Type;
import org.vesalainen.comm.channel.CommStatus;
import org.vesalainen.comm.channel.SerialChannel;
import org.vesalainen.comm.channel.winx.WinSerialChannel;
import java.io.IOException;
import java.io.InputStream;

class Reader
{

    public static void main(String... args)
    {
        SerialChannel.debug(true);
        
        RandomChar random = new RandomChar();

        String port = "COM1";
        if (args.length > 0)
        {
            port = args[0];
        }
        for (String com : WinSerialChannel.getAllPorts())
        {
            System.err.println(com);
        }
        try
        {
            EventPrinter ep = new EventPrinter();
            TestEnumerator en = new TestEnumerator(port);
            SerialChannel sc = en.next();
            while (sc != null)
            {
                sc.addEventObserver(ep, Type.BREAK, Type.CHAR, Type.CTS, Type.DSR, Type.EMPTY, Type.ERROR, Type.FLAG, Type.RING, Type.RLSD);
                CommStatus cs = sc.getCommStatus();
                System.err.println(cs);
                InputStream in = sc.getInputStream(1024);
                int count = 0;
                boolean ok = true;
                while (count < TestEnumerator.count())
                {
                    int next = random.next(sc.getDataBits().ordinal()+4);
                    int cc = in.read();
                    if (cc != next)
                    {
                        ok = false;
                    }
                    if (!ok)
                    {
                        System.err.println(count+": "+Integer.toBinaryString(cc)+" <> "+Integer.toBinaryString(next));
                    }
                    count++;
                }
                if (!ok)
                {
                    System.err.println("failed! "+sc);
                }
                else
                {
                    System.err.println(sc);
                }
                sc.close();
                sc = en.next();
                random.reset();
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
