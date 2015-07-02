/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javax.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tkv
 */
public class CommPortIdentifierT
{
    
    public CommPortIdentifierT()
    {
    }

    @Test
    public void test1()
    {
        try
        {
            Enumeration en = CommPortIdentifier.getPortIdentifiers();
            List<CommPortIdentifier> ports = new ArrayList<>();
            while (en.hasMoreElements())
            {
                CommPortIdentifier cp = (CommPortIdentifier) en.nextElement();
                ports.add(cp);
            }
            assertTrue("not enough ports for the test", ports.size() >= 2);
            CommPortIdentifier cpi1 = ports.get(0);
            SerialPort sp1 = (SerialPort) cpi1.open("app1", 0);
            assertNotNull(sp1);
            
            CommPortIdentifier cpi2 = ports.get(1);
            SerialPort sp2 = (SerialPort) cpi2.open("app2", 0);
            assertNotNull(sp2);
            
            sp1.setSerialPortParams(4800, SerialPort.DATABITS_5, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            assertEquals(4800, sp1.getBaudRate());
            assertEquals(SerialPort.DATABITS_5, sp1.getDataBits());
            assertEquals(SerialPort.STOPBITS_1, sp1.getStopBits());
            assertEquals(SerialPort.PARITY_NONE, sp1.getParity());
            
            sp1.setSerialPortParams(9600, SerialPort.DATABITS_6, SerialPort.STOPBITS_1_5, SerialPort.PARITY_EVEN);
            assertEquals(9600, sp1.getBaudRate());
            assertEquals(SerialPort.DATABITS_6, sp1.getDataBits());
            assertEquals(SerialPort.STOPBITS_1_5, sp1.getStopBits());
            assertEquals(SerialPort.PARITY_EVEN, sp1.getParity());
            
            sp1.setSerialPortParams(19200, SerialPort.DATABITS_7, SerialPort.STOPBITS_2, SerialPort.PARITY_ODD);
            assertEquals(19200, sp1.getBaudRate());
            assertEquals(SerialPort.DATABITS_7, sp1.getDataBits());
            assertEquals(SerialPort.STOPBITS_2, sp1.getStopBits());
            assertEquals(SerialPort.PARITY_ODD, sp1.getParity());
            
            sp1.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_2, SerialPort.PARITY_MARK);
            assertEquals(38400, sp1.getBaudRate());
            assertEquals(SerialPort.DATABITS_8, sp1.getDataBits());
            assertEquals(SerialPort.STOPBITS_2, sp1.getStopBits());
            assertEquals(SerialPort.PARITY_MARK, sp1.getParity());
            
            sp1.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_2, SerialPort.PARITY_SPACE);
            assertEquals(38400, sp1.getBaudRate());
            assertEquals(SerialPort.DATABITS_8, sp1.getDataBits());
            assertEquals(SerialPort.STOPBITS_2, sp1.getStopBits());
            assertEquals(SerialPort.PARITY_SPACE, sp1.getParity());
            
            sp1.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
            assertEquals(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT, sp1.getFlowControlMode());
            sp1.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT);
            assertEquals(SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT, sp1.getFlowControlMode());
            sp1.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            assertEquals(SerialPort.FLOWCONTROL_NONE, sp1.getFlowControlMode());
            
            sp2.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_2, SerialPort.PARITY_SPACE);
            
            OutputStream os = sp1.getOutputStream();
            InputStream is = sp2.getInputStream();
            String exp = "Hello world";
            os.write(exp.getBytes());
            os.flush();
            byte[] buf = new byte[256];
            int rc = is.read(buf);
            assertTrue(rc != -1);
            int off = rc;
            while (off < 11)
            {
                assertTrue(rc != -1);
                rc = is.read(buf, off, buf.length-off);
                off += rc;
            }
            assertEquals(11, off);
            String got = new String(buf, 0, off);
            assertEquals(exp, got);
        }
        catch (IOException | PortInUseException | UnsupportedCommOperationException ex)
        {
            fail(ex.getMessage());
            Logger.getLogger(CommPortIdentifierT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
