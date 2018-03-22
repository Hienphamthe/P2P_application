package main.java.SIP;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.sdp.*;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

//import org.apache.commons.*;

import java.text.ParseException;
import java.util.*;
import main.java.Debian.NodeProperties;


public class IMS implements SipListener {
    private static AddressFactory addressFactory;
    private static MessageFactory messageFactory;
    private static HeaderFactory headerFactory;
    private static SipStack sipStack;
    private static SipProvider udpProvider;
    private ListeningPoint udpListeningPoint ;
    private static String transport;
    
    public static int myPort;

    @Override
    public void processDialogTerminated(DialogTerminatedEvent arg0) {
            // TODO Auto-generated method stub	
    }

    @Override
    public void processIOException(IOExceptionEvent arg0) {
            // TODO Auto-generated method stub	
    }

    @Override
    public void processResponse(ResponseEvent evt) {         
        Response response = evt.getResponse();         
        int status = response.getStatusCode();          
        System.out.println("Received SIP Response:  "+ status);
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent.getServerTransaction();
        System.out.println("Received SIP Request: " + request.getMethod());
        if(request.getMethod().equals(Request.MESSAGE)){
            processMessage(requestEvent, serverTransactionId);
        }else{
            try {
                serverTransactionId.sendResponse(messageFactory.createResponse(405, request)); 
            }
            catch (SipException e){ 
                e.printStackTrace();
            }
            catch (InvalidArgumentException e) {
                e.printStackTrace();
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void processMessage(RequestEvent requestEvent,ServerTransaction serverTransactionID) {
        SipProvider sipProvider = (SipProvider)requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try{
            String message = new String(request.getRawContent());
            FromHeader fromHeader = (FromHeader) request.getHeader("From");
            String fromTempURI = fromHeader.getAddress().toString();
            ToHeader toHeader = (ToHeader) request.getHeader("To");
            String toTempURI = toHeader.getAddress().toString();

            Response okResponse = messageFactory.createResponse(200, request);

            serverTransactionID = sipProvider.getNewServerTransaction(request);
            serverTransactionID.sendResponse(okResponse);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void processTimeout(TimeoutEvent arg0) {
        System.out.println("Transaction Time out");
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
            // TODO Auto-generated method stub
    }

    public void sendMessage(String tempURI, String message){
        try {
//             create & extract username and address
            String fromName = NodeProperties.userName;
            String fromSipAddress = NodeProperties.host+".de";
            String fromDisplayName = NodeProperties.userName;
             // extract receiver name & address from tempURI aaa@192.168.1.1
            String toUserTemp = tempURI.substring(0, tempURI.indexOf("@"));
            String toSipAddressTemp = tempURI.substring(tempURI.indexOf("@")+1);
            
            SipURI fromAddress = addressFactory.createSipURI(fromName, NodeProperties.p2pIP);
            Address fromNameAddress = addressFactory.createAddress(fromAddress);
            fromNameAddress.setDisplayName(fromDisplayName);
            FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, new Long(new Random().nextLong()).toString());

            SipURI toAddress = addressFactory.createSipURI(toUserTemp, toSipAddressTemp);
            Address toNameAddress = addressFactory.createAddress(toAddress);
            toNameAddress.setDisplayName(toUserTemp);
            ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

            SipURI requestURI = addressFactory.createSipURI(toUserTemp, toSipAddressTemp+":"+myPort);
            requestURI.setTransportParam("udp");

            ArrayList viaHeaders = new ArrayList();
            int port = udpProvider.getListeningPoint(transport).getPort();
            ViaHeader viaHeader = headerFactory.createViaHeader(NodeProperties.p2pIP, port, transport, null);
            viaHeaders.add(viaHeader);

            CallIdHeader callIdHeader = udpProvider.getNewCallId();
            
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.MESSAGE);

            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

            Request request =  messageFactory.createRequest(
                    requestURI, Request.MESSAGE, callIdHeader, cSeqHeader,
                    fromHeader, toHeader, viaHeaders, maxForwards);

            String host = NodeProperties.p2pIP;
            SipURI contactURI = addressFactory.createSipURI(fromName, host);
            contactURI.setPort(port);
            
            Address contactAddress = addressFactory.createAddress(contactURI);
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");
            request.setContent(message, contentTypeHeader);

            udpProvider.sendRequest(request);

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

//    public static void main (String[] arg){
//        new IMS().init();
//        new IMS().sendMessage("abc@127.0.0.1", "abcxyz");
//    }
    
    public void init(){
        SipFactory sipFactory = null;
        sipStack = null;
        udpProvider = null;
        transport = "udp";
        
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();

        properties.setProperty("javax.sip.STACK_NAME", "Instance Messaging");
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "NONE");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "SipApplication_debug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG",	"SipApplication_log.txt");

        try {
            sipStack = sipFactory.createSipStack(properties);

        } catch (PeerUnavailableException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            if (e.getCause() != null)
                e.getCause().printStackTrace();
            System.exit(0);
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();

            IMS ims = new IMS();
            ims.createProvider();
            ims.udpProvider.addSipListener(ims);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private void createProvider() throws Exception {
        this.udpListeningPoint = sipStack.createListeningPoint(NodeProperties.p2pIP, myPort, transport);
        udpProvider = sipStack.createSipProvider(udpListeningPoint);
    }
}
