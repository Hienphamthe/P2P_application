package main.java.CoAP;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import main.java.Debian.NodeProperties;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;


public class Resource extends CoapServer {
    private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    
    public static boolean _PUTreceived;
    public static CoapExchange exg;
    public static String message;
    
    public void init () {
        _PUTreceived = false;
        LogManager.getLogManager().reset();
        Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        globalLogger.setLevel(java.util.logging.Level.OFF);
        try {
            // create server
            Resource server = new Resource();
            // add endpoints on all IP addresses
            server.addEndpoints();
            server.start();
        } catch (SocketException e) {
            System.err.println("Failed to initialize server: " + e.getMessage());
        }
    }
    
    private void addEndpoints() {
    	for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
            // only binds to IPv4 addresses and localhost
            if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
                InetSocketAddress bindToAddress = new InetSocketAddress(addr, COAP_PORT);
                addEndpoint(new CoapEndpoint(bindToAddress));
            }
        }
    }
    
    public Resource() throws SocketException {
        // provide an instance of a resource
        add(new NotifierResource());
    }
    
    class NotifierResource extends CoapResource {        
        public NotifierResource() {
            // set resource identifier
            super(NodeProperties.userName);            
        }
        
        @Override
        public void handlePUT (CoapExchange exchange) {
            exg = exchange;
            message = exchange.getRequestText().trim();
            System.out.println("Received PUT messages: "+message);
            
            _PUTreceived = true;      
        }
    }
     
    public void sendAccepted (String message) {
            exg.respond(ResponseCode.CREATED, message);
        }
    
    public void sendOK (String message) {
         exg.respond(ResponseCode.CHANGED, message);
     }
}
