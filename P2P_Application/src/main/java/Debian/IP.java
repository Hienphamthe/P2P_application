package main.java.Debian;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IP {
    private NetworkInterface ni;

    public String getIP(String adapter) {

        //Only works on LINUX-Based systems... windows needs to be added
        try {
            ni = NetworkInterface.getByName(adapter);
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress ia = inetAddresses.nextElement();
                if (!ia.isLinkLocalAddress()) {
                        return ia.getHostAddress().toString();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }	
}
