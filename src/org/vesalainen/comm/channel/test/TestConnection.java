/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.vesalainen.comm.channel.test;

import java.io.InputStream;
import org.vesalainen.comm.channel.SerialChannel;
import org.vesalainen.comm.channel.SerialChannel.Speed;

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
            
            String port = null;
            Speed speed = null;
            if (args.length < 2)
            {
                System.err.println("usage: port speed");
                System.exit(0);
                //port = "COM1";
                //speed = Speed.CBR_4800;
            }
            else
            {
                port = args[0];
                speed = Speed.valueOf("CBR_"+args[1]);
            }
            SerialChannel sc = SerialChannel.getInstance(port, speed);
            sc.connect();
            InputStream is = sc.getInputStream(100);
            int cc = is.read();
            while (cc > 0)
            {
                System.out.print((char)cc);
                cc = is.read();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
