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

import org.vesalainen.comm.channel.winx.WinSerialChannel;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.vesalainen.comm.channel.SerialChannel;

/**
 *
 * @author tkv
 */
public class Simulator
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            if (args.length < 3)
            {
                System.err.println("usage: ... Simulator <port> <speed> <file>");
            }
            else
            {
                String port = args[0];
                int speed = Integer.parseInt(args[1]);
                File file = new File(args[2]);
                FileInputStream fis = new FileInputStream(file);
                SerialChannel sc = SerialChannel.getInstance(port, speed);
                FileChannel fc = fis.getChannel();
                ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                System.out.println("Writing contents of "+file+" to "+port+" at speed "+speed);
                sc.write(bb);
                sc.close();
                System.out.println("Completed...");
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
