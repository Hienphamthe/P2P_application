package main.java.CoAP;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class PutClient {
    public static boolean _allowEvaluate;
    public static String content;
    public void init(String tempIP, String message) {
        LogManager.getLogManager().reset();
        Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        globalLogger.setLevel(java.util.logging.Level.OFF);
        
        // extract receiver name & address from permURI aaa@TKLAB.de
        String toUserPerm = tempIP.substring(0, tempIP.indexOf("@"));
        
        URI uri = null; // URI parameter of the request
        try {
            uri = new URI("coap://"+tempIP+":5683/"+toUserPerm);
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI: " + e.getMessage());
            System.exit(-1);
        }
        
        CoapClient client = new CoapClient(uri);
        
        client.put(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                System.out.println("Received response code: "+response.getCode());
//                System.out.println(response.getOptions());
                content = response.getResponseText();           
                System.out.println(content);
                
                    _allowEvaluate = true;
                
//                System.out.println("\nADVANCED\n");
//                System.out.println(Utils.prettyPrint(response));
            }
            @Override
            public void onError() {
                System.err.println("FAILED");
            }
        }, message, MediaTypeRegistry.TEXT_PLAIN);
    }
}
