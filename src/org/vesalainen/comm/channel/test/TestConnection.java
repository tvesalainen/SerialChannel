/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vesalainen.comm.channel.test;

import java.io.InputStream;
import org.vesalainen.comm.channel.CommEvent;
import org.vesalainen.comm.channel.SerialChannel;
import org.vesalainen.comm.channel.SerialChannel.Speed;
import org.vesalainen.comm.channel.winx.WinSerialChannel;

/**
 * @author Timo Vesalainen
 */
public class TestConnection
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)    
    {
        try
        {
            
            for (String com : SerialChannel.getAllPorts())
            {
                System.err.println(com);
            }
            String port = null;
            Speed speed = null;
            if (args.length < 2)
            {
                //System.err.println("usage: port speed");
                //System.exit(0);
                port = "COM3";
                speed = Speed.CBR_38400;
            }
            else
            {
                port = args[0];
                speed = Speed.valueOf("CBR_"+args[1]);
            }
            SerialChannel sc = SerialChannel.getInstance(port, speed);
            //WinSerialChannel.debug(true);
            sc.connect();
            //sc.addEventObserver(new EventPrinter(), CommEvent.Type.values());
            InputStream is = sc.getInputStream(10);
            int cc = is.read();
            while (true)
            {
                System.out.print((char)cc);
                cc = is.read();
            }
            //System.out.println(cc);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
