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
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * This utility opens a SerialChannel and send pseudo random characters.
 * @author tkv
 */
public class RegressionTester
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            String port = "COM1";
            if (args.length > 0)
            {
                port = args[0];
            }
            PrintWriter pw = new PrintWriter("c:\\temp\\writer.txt");
            TestEnumerator en = new TestEnumerator(port);
            RandomChar random = new RandomChar();
            SerialChannel sc = en.next();
            while (sc != null)
            {
                OutputStream out = sc.getOutputStream(321);
                
                int count = 0;
                int next = 0;
                while (count < TestEnumerator.count())
                {
                    next = random.next(sc.getDataBits().ordinal()+4);
                    System.err.println(count+": "+next);
                    out.write(next);
                    count++;
                }
                out.flush();
                sc.close();
                pw.close();
                Thread.sleep(1000);
                sc = en.next();
                count = 0;
                random.reset();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
