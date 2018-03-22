package main.java.SIP;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import java.util.*;
import main.java.Debian.NodeProperties;



public class Notifier implements SipListener {

    private static AddressFactory addressFactory;
    private static MessageFactory messageFactory;
    private static HeaderFactory headerFactory;
    private static SipStack sipStack;
    private static SipProvider udpProvider;
    public static RequestEvent requestEvent;
    public static ServerTransaction serverTransactionId;
    
    public static boolean _202Accepted;
    public static int myPort;
    
    private ListeningPoint udpListeningPoint ;
    private static String transport;
    public static Dialog dialog;
    public static EventHeader eventHeader;
    
    // Process SUBSCRIBE message and send back NOTIFY message
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        ServerTransaction serverTransactionId = requestEvent.getServerTransaction();
        this.requestEvent = requestEvent;
        this.serverTransactionId = serverTransactionId;
//        System.out.println("\n\n*****Got a request*****");
//        System.out.println(
//                "Request " 
//                        + request.getMethod()
//                        + " received at " 
//                        + sipStack.getStackName()
//                        + " with server transaction id " 
//                        + serverTransactionId
//                        + " and dialog id " 
//                        + requestEvent.getDialog());
//        System.out.println(request);

        if (request.getMethod().equals(Request.SUBSCRIBE)) {
            _202Accepted = true;
        }
    }
    
    // Process SUBSCRIBE message
    public ServerTransaction processSubscribe(String message) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        
        try {
            Response response = null;
            
//            System.out.println("Dialog = " + requestEvent.getDialog());
            if (serverTransactionId == null) {
                serverTransactionId = sipProvider.getNewServerTransaction(request);
            }
            dialog = serverTransactionId.getDialog();
            response = messageFactory.createResponse(202, request);

            // add event header
//            eventHeader = (EventHeader) request.getHeader(EventHeader.NAME);
//            if ( eventHeader == null) {
//                System.out.println("Cannot find event header.... dropping request.");
//            }
//            response.addHeader(eventHeader);
            
            // add headers from SUBSCRIBE message
            ExpiresHeader expires = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
            if (expires == null) {
                expires = headerFactory.createExpiresHeader(3600);// 1 h
            }
            if (expires.getExpires() == 0) {
//                System.out.println("Deleting Dialog ");
                serverTransactionId.getDialog().delete();
            }
            response.addHeader(expires);
            
            // create contact header
            String host = NodeProperties.p2pIP;
            SipURI contactURI = addressFactory.createSipURI(NodeProperties.userName, host);
            contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());
            Address contactAddress = addressFactory.createAddress(contactURI);
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            response.addHeader(contactHeader);
            
            // add To header
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            if (toHeader.getTag() == null) {
                String tag = new Long(new java.util.Random().nextLong()).toString();
                toHeader.setTag(tag);
            }
            
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");
            response.setContent(message, contentTypeHeader);
            
            // send out 202 accepted response
            serverTransactionId.sendResponse(response);
//            System.out.println("Dialog State is: "+ serverTransactionId.getDialog().getState());
//            System.out.println("\n*****Sent a 202 Accept response*****");
//            System.out.println(response);
            return serverTransactionId;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        return null;
    }
    
    // Send back NOTIFY message
    public void sendNotify(Dialog dialog, String message) {
        try {
            Request notifyRequest = dialog.createRequest("NOTIFY");

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
            notifyRequest.setHeader(maxForwards);

            // Create contact headers
            String host = NodeProperties.p2pIP;

            SipURI contactUrl = addressFactory.createSipURI(NodeProperties.userName, host);
            contactUrl.setLrParam();
            contactUrl.setPort(udpProvider.getListeningPoint(transport).getPort());

            Address contactAddress = addressFactory.createAddress(contactUrl);

            notifyRequest.setHeader(headerFactory.createContactHeader(contactAddress));          
               
            // Create subscription header
            SubscriptionStateHeader subscriptionStateHeader = headerFactory.createSubscriptionStateHeader("active");
//            subscriptionStateHeader.setExpires(20000);
            
            notifyRequest.addHeader(subscriptionStateHeader);
//            notifyRequest.setHeader(eventHeader);
            
            // read message content
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");
            notifyRequest.setContent(message, contentTypeHeader);
            
            // send the request out.
            ClientTransaction ct = udpProvider.getNewClientTransaction(notifyRequest);
            dialog.sendRequest(ct);
            
//            System.out.println("Dialog ID: " + dialog);            
//            System.out.println("\n*****Sent a NOTIFY request*****");
//            System.out.println(notifyRequest+"\n");

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    // Process 200 OK
    @Override
    public synchronized void processResponse(ResponseEvent responseReceivedEvent) {
//        System.out.println("\n\n*****Got a response: *****");
        Response response = (Response) responseReceivedEvent.getResponse();
        Transaction tid = responseReceivedEvent.getClientTransaction();

//        System.out.println("Response received with client transaction id "+ tid + "is: "+ response.getStatusCode());
//        System.out.println(response);
        if (tid == null) {
//            System.out.println("Stray response -- dropping ");
        return;
        }
//        System.out.println("Transaction state = " + tid.getState());
//        System.out.println("Dialog = " + tid.getDialog());
        if ( tid.getDialog () != null ){
//            System.out.println("Dialog State = " + tid.getDialog().getState());
        }
    }

    @Override
    public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {
        Transaction transaction;
        if (timeoutEvent.isServerTransaction()) {
            transaction = timeoutEvent.getServerTransaction();
        } else {
            transaction = timeoutEvent.getClientTransaction();
        }
        System.out.println("state = " + transaction.getState());
        System.out.println("dialog = " + transaction.getDialog());
        System.out.println("dialogState = "+ transaction.getDialog().getState());
        System.out.println("Transaction Time out");
    }

    public void createProvider() throws Exception {
        this.udpListeningPoint = sipStack.createListeningPoint(NodeProperties.p2pIP, myPort, transport);
        udpProvider = sipStack.createSipProvider(udpListeningPoint);
    }
    
    public void init() {
        SipFactory sipFactory = null;
        sipStack = null;
        transport = "udp";
        _202Accepted = false;
        
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        
//        properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort + "/"+ transport);
        properties.setProperty("javax.sip.STACK_NAME", "notifier");
        // You need 16 for logging traces. 32 for debug + traces.
        // Your code will limp at 32 but it is best for debugging.
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "NONE");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","shootmedebug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG","shootmelog.txt");
//        properties.setProperty("gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME","20");

        try {
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
//            System.out.println("sipStack = " + sipStack);
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
            
            Notifier notifier = new Notifier();
            notifier.createProvider();
            notifier.udpProvider.addSipListener(notifier);
//            System.out.println("udp provider = " + udpProvider);
            
//            notifier.udpProvider.addSipListener(notifier);
//            ListeningPoint lp = sipStack.createListeningPoint(NodeProperties.p2pIP,, "udp");
//            Notifier listener = this;
//            udpProvider = sipStack.createSipProvider(lp);
//            System.out.println("udp provider " + udpProvider);
            
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

//    public static void main(String args[]) {
//        new Notifier().init();
//    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        System.out.println("IOException");

    }
    @Override
    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
//        System.out.println("Transaction terminated event recieved");

    }
    @Override
    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        System.out.println("Dialog terminated event recieved");
    }
}
