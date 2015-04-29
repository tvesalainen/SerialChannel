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
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Opens a serial channel. Echoes all characters back.
 * @author tkv
 */
public class Echo extends WinSerialChannel implements Runnable
{
    private Thread thread;
    public Echo(String port, Speed speed) throws IOException
    {
        super(port, speed);
        thread = new Thread(this);
        thread.start();
    }

    public Echo(String port, Speed speed, Parity parity, DataBits dataBits, StopBits stopBits, FlowControl flowControl) throws IOException
    {
        super(port, speed);
        setParity(parity);
        setDataBits(dataBits);
        setStopBits(stopBits);
        setFlowControl(flowControl);
        thread = new Thread(this);
        thread.start();
    }


    public void interrupt()
    {
        thread.interrupt();
    }

    public void run()
    {
        ByteBuffer buf = ByteBuffer.allocateDirect(4096);
        while (true)
        {
            try
            {
                buf.clear();
                read(buf);
                buf.flip();
                write(buf);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                return;
            }
        }
    }
}
