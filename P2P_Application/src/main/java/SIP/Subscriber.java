package main.java.SIP;
import main.java.Debian.NodeProperties;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import java.util.*;


public class Subscriber implements SipListener {

    private static AddressFactory addressFactory;
    private static MessageFactory messageFactory;
    private static HeaderFactory headerFactory;
    
    private static SipStack sipStack;
    private static SipProvider sipProvider;
    private static String transport;
    
    public static boolean _allowCompare;
    public static boolean _allowEvaluate;
    public static int myPort;
    public static String message;
    
    private ListeningPoint udpListeningPoint ;
    private Dialog subscriberDialog;
    private SipFactory sipFactory = null;
    private ContactHeader contactHeader;
    private ClientTransaction subscribeTid;
    
    private int seq_num = 1;
                                    
    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        System.out.println("IOException happened for "
                + exceptionEvent.getHost() + " port = "
                + exceptionEvent.getPort());
    }
    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
//        System.out.println("Transaction terminated event recieved");
    }
    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        System.out.println("Dialog terminated event recieved");
    }
    @Override
    public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {
        System.out.println("Transaction Time out");
    }
    
    // Process request message
    @Override
    public void processRequest(RequestEvent requestReceivedEvent) {
        Request request = requestReceivedEvent.getRequest();
        ServerTransaction serverTransactionId = requestReceivedEvent.getServerTransaction();
        String viaBranch = ((ViaHeader)(request.getHeaders(ViaHeader.NAME).next())).getParameter("branch");
        
//        System.out.println("\n\n*****Got a request*****");
//        System.out.println(
//            "Request "
//                + request.getMethod()
//                + " received at "
//                + sipStack.getStackName()
//                + " with server transaction id "
//                + serverTransactionId 
//                + " branch ID = " 
//                + viaBranch
//                + " and dialog id " 
//                + requestReceivedEvent.getDialog());
//        System.out.println(request);

        if (request.getMethod().equals(Request.NOTIFY))
            processNotify(requestReceivedEvent, serverTransactionId);
        
        // Extract information from NOTIFY message
            message = null;
            message = new String(request.getRawContent());
            System.out.println(message);
            
            _allowCompare = false;
            if (message != null) {
//                System.out.println("_allowCompare is set to true");
                _allowCompare = true;
            }
//            else {
//                System.out.println("_allowCompare is set to false");
//                _allowCompare = false;
//            }
            
    }

    public synchronized void processNotify(RequestEvent request, ServerTransaction serverTransactionId) {
        SipProvider provider = (SipProvider) request.getSource();
        Request notify = request.getRequest();
        
        try {
            if (serverTransactionId == null) {
//                System.out.println("subscriber:  null TID.");
                serverTransactionId = provider.getNewServerTransaction(notify);
            }
            Dialog dialog = serverTransactionId.getDialog();
//            System.out.println("Dialog = " + dialog);
            if (dialog != null) {
//                System.out.println("Dialog State = " + dialog.getState());
            }
            
            Response response = messageFactory.createResponse(200, notify);
            
            String fromName = NodeProperties.userName;
            String host = NodeProperties.p2pIP;
            SipURI contactURI = addressFactory.createSipURI(fromName, host);
            int port = sipProvider.getListeningPoint(transport).getPort();
            contactURI.setPort(port);
            Address contactAddress = addressFactory.createAddress(contactURI);
            ContactHeader contact = headerFactory.createContactHeader(contactAddress);
            ((SipURI)contact.getAddress().getURI()).setParameter( "id", "sub" );
            response.addHeader( contact );
           
//            System.out.println("Transaction State = " + serverTransactionId.getState());
            SubscriptionStateHeader subscriptionState =  (SubscriptionStateHeader)notify.getHeader(SubscriptionStateHeader.NAME);
            //  Subscription is terminated.
            if ( subscriptionState.getState().equals(SubscriptionStateHeader.TERMINATED)) {
//                System.out.println("Delete dialog");
                dialog.delete();
            }
            else {
//                System.out.println("Subscription state now: " + subscriptionState.getState());
            }
            
            serverTransactionId.sendResponse(response);
//            System.out.println("\n*****Sent a response back*****");
//            System.out.println(response);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    // Process 202 Accept
    @Override
    public void processResponse(ResponseEvent responseReceivedEvent) {
//        System.out.println("\n\n*****Got a response*****");
        Response response = (Response) responseReceivedEvent.getResponse();
        Transaction tid = responseReceivedEvent.getClientTransaction();

        System.out.println("Response received with client transaction id: "+ tid+ " is: "+ response.getStatusCode());
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
        
        message = null;
        message = new String(response.getRawContent());
        System.out.println(message);
        if (response.getStatusCode() == 202){
            _allowEvaluate = true;
        }
    }

    public void sendSubscribe(String tempURI, String permURI) {
        try {
            // create & extract username and address
            String fromName = NodeProperties.userName;
            String fromSipAddress = NodeProperties.host+".de";
            String fromDisplayName = NodeProperties.userName;
             // extract receiver name & address from tempURI aaa@192.168.1.1
            String toUserTemp = tempURI.substring(0, tempURI.indexOf("@"));
            String toSipAddressTemp = tempURI.substring(tempURI.indexOf("@")+1);
            // extract receiver name & address from permURI aaa@TKLAB.de
            String toUserPerm = permURI.substring(0, permURI.indexOf("@"));
            String toSipAddressPemp = permURI.substring(permURI.indexOf("@")+1);

            // create From Header
            SipURI fromAddress = addressFactory.createSipURI(fromName, fromSipAddress);
            Address fromNameAddress = addressFactory.createAddress(fromAddress);
            fromNameAddress.setDisplayName(fromDisplayName);
            FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, new Long(new Random().nextLong()).toString());
            
            // create To Header
            SipURI toAddress = addressFactory.createSipURI(toUserPerm, toSipAddressPemp);
            Address toNameAddress = addressFactory.createAddress(toAddress);
            toNameAddress.setDisplayName(toUserPerm);
            ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

//            System.out.println("\nSending from "+fromAddress+" a SUBSCRIBE message to: "+toAddress+" ....");

            // create Request URI
            SipURI requestURI = addressFactory.createSipURI(toUserTemp, toSipAddressTemp+":"+myPort);

            // Create ViaHeaders
            ArrayList viaHeaders = new ArrayList();
            int port = sipProvider.getListeningPoint(transport).getPort();
            ViaHeader viaHeader = headerFactory.createViaHeader(NodeProperties.p2pIP,port, transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();

            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader((long) seq_num, Request.SUBSCRIBE);
            seq_num++;

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

            // Create the request.
            Request request =
                messageFactory.createRequest(
                    requestURI,
                    Request.SUBSCRIBE,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwards);
            // Create contact headers
            String host = NodeProperties.p2pIP;

            SipURI contactURI = addressFactory.createSipURI(fromName, host);
            contactURI.setPort(port);

            Address contactAddress = addressFactory.createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Expire header in s
//            ExpiresHeader expires_header = headerFactory.createExpiresHeader(60);
//            request.addHeader(expires_header);

            // Event header
            EventHeader event_header = headerFactory.createEventHeader("Event");
            event_header.setEventId("Sub-notify");
            request.addHeader(event_header);
            
            // Create the client transaction.
            subscribeTid = sipProvider.getNewClientTransaction(request);
            this.subscriberDialog = subscribeTid.getDialog();
//            System.out.println("Dialog ID: " + subscriberDialog);

    //        ClientTransaction ct = sipProvider.getNewClientTransaction(request);
    //        ct.sendRequest();

            // send the request out.
            subscribeTid.sendRequest();
//            System.out.println("\n*****Sent a request*****");
//            System.out.println(request+"\n");
            
        } catch (Throwable ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
        public void createProvider() throws Exception {
        this.udpListeningPoint = sipStack.createListeningPoint(NodeProperties.p2pIP, myPort, transport);
        sipProvider = sipStack.createSipProvider(udpListeningPoint);
    }
    
    public void init(){
        SipFactory sipFactory = null;
        sipStack = null;
        sipProvider = null;
        _allowCompare = false;
        transport = "udp";

        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();

    //properties.setProperty("javax.sip.OUTBOUND_PROXY",toSipAddressTemp+":"+myPort+"/" +transport);
        properties.setProperty("javax.sip.STACK_NAME", "subscriber");
    //properties.setProperty("javax.sip.MAX_MESSAGE_SIZE", "1048576");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","subscriberdebug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG","subscriberlog.txt");
    //Drop the client connection after we are done with the transaction.
    //properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "NONE");
    //properties.setProperty("gov.nist.javax.sip.MAX_LISTENER_RESPONSE_TIME","20");

        try {
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
//            System.out.println("sipStack = " + sipStack);
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(0);
        }

        // start creating SUBSCRIBE request
        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();

            Subscriber subscriber = new Subscriber();
            subscriber.createProvider();
            subscriber.sipProvider.addSipListener(subscriber);
            
//            udpListeningPoint = sipStack.createListeningPoint(NodeProperties.p2pIP,myPort, transport);
//            sipProvider = sipStack.createSipProvider(udpListeningPoint);
//            Subscriber listener = this;
//            sipProvider.addSipListener(listener);
            
//            System.out.println("udp provider = " + sipProvider);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }    
    }
}
