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
import org.vesalainen.comm.channel.winx.WinSerialChannel;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.ByteBuffer;

/**
 *
 * @author tkv
 */
public class Tester1
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            WinSerialChannel.Speed[] speeds = WinSerialChannel.Speed.values();
            WinSerialChannel.FlowControl[] flows = WinSerialChannel.FlowControl.values();
            LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
            for (SerialChannel.Speed speed : speeds)
            {
                for (SerialChannel.Parity parity: SerialChannel.Parity.values())
                {
                    for (SerialChannel.DataBits databits : SerialChannel.DataBits.values())
                    {
                        for (SerialChannel.StopBits stopbits : SerialChannel.StopBits.values())
                        {
                            for (SerialChannel.FlowControl flow : flows)
                            {
                                SerialChannel sc = null;
                                String conf = speed.toString()+" "+parity.toString()+" "+databits.toString()+" "+stopbits.toString()+" "+flow.toString()+"\r\n";
                                try
                                {
                                    sc = SerialChannel.getInstance("COM8"
                                            + ""
                                            + "", speed, parity, databits, stopbits, flow);
                                    sc.connect();

                                }
                                catch (IOException ex)
                                {
                                    System.err.println("Skipping: "+conf+" "+ex.getMessage());
                                    continue;
                                }
                                System.err.println(conf);
                                in.readLine();
                                ByteBuffer bb = ByteBuffer.allocateDirect(100);
                                bb.put(conf.getBytes());
                                bb.flip();
                                sc.write(bb);
                                bb = ByteBuffer.wrap(conf.getBytes());
                                sc.write(bb);
                                bb = ByteBuffer.allocateDirect(100);
                                bb.position(2);
                                bb.limit(3);

                                sc.read(bb);
                                bb.position(2);
                                System.err.println((char)bb.get());
                                sc.close();
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
