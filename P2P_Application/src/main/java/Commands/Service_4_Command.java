package main.java.Commands;

import java.io.IOException;
import main.java.SIP.Subscriber;
import main.java.CoAP.PutClient;

import de.uniba.wiai.lspi.chord.service.ServiceException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import javax.sip.Dialog;
import static main.java.CoAP.PutClient.content;
import main.java.CoAP.Resource;
import main.java.Debian.ChatClient;
import static main.java.Debian.ChatClient.readCommand;
import main.java.Debian.NodeProperties;
import main.java.Debian.PresenceService;
import main.java.MQTT.Broker;
import main.java.MQTT.SubsClient;
import main.java.SIP.Notifier;


public class Service_4_Command implements ChordCommand {
    private String _commandParts;
    private PresenceService _chordManager;
    private String _username;

    private static String acceptedMessage;
    private static String receiver;
    private static String notifyMessage;
    
    private static List<Dialog> dialogList;
    private static List<String> notifierList;
    
    private static List<String> subsIPList;
    
    private static List<String> phonerLiteList;
    
    private static boolean _configurationDone;

    public Service_4_Command(List<String> notifierList, PresenceService chordManager,String userURI) {
            _chordManager = chordManager;
            _username = userURI;
            this.notifierList = notifierList;
            
    }

    @Override
    public void execute() throws IOException, ServiceException {
        // SIP
        if (ChatClient.protocol.equals("1")){
            executeSIP();
        }
        //CoAP
        else if (ChatClient.protocol.equals("2")) {
            executeCoAP();       
        }
        // MQTT
        else if (ChatClient.protocol.equals("3")) {
            executeMQTT();
        }
    } 
    
    private void executeSIP() throws IOException, ServiceException {
        dialogList = new ArrayList<>();
        phonerLiteList = new ArrayList<>();
        Subscriber.myPort = 5060;
        Notifier.myPort = 5070; 
        
        // initialize subscriber
        new Subscriber().init();
        for (int i=0; i < notifierList.size();i++){
            _commandParts = notifierList.get(i);
            
            String receiverPermURI = _commandParts;
            System.out.println("Looking for receiverPermURI: "+receiverPermURI+"\n");
            String receiverTempURI = _chordManager.lookup(receiverPermURI);
            if (receiverTempURI == null)
                System.out.println("The receiver does NOT exist! or wrong permURI, {e.g: Debian05@TKLAB.de} ");
            else if (receiverTempURI.equals(_username))
                System.out.println("You can NOT send a message to yourself.");
            else {
                new Subscriber().sendSubscribe(receiverTempURI, receiverPermURI);
            }
        }
        // initialize notifier
        new Notifier().init();
        // response once for evaluate sensor format
        do {
            try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            // execute when receive response from SUBSCRIBE
            if (Subscriber._allowEvaluate == true){
                String[] receivedMessage = Subscriber.message.split(" ");
                for (int i=0; i< receivedMessage.length; i++){
                    String[] messageParts = receivedMessage[i].split(":");
                    String _sensorID = messageParts[0];
                    String _sensorFormat = messageParts[1];

                    if (_sensorFormat.equals("Boolean")){
                        System.out.println("Input Phonerlite Receiver URI for water detection in form abc@192.168.50.11: ");
                        receiver =  new ChatClient().readCommand();
                    } else if (_sensorFormat.equals("String")) {
                        System.out.println("Input Phonerlite Receiver URI for water temperature in form abc@192.168.50.11: ");
                        receiver =  new ChatClient().readCommand();                        
                    }
                    
                    if (acceptedMessage == null){
                        acceptedMessage = receivedMessage[i]+":"+receiver;                            
                    } else {
                        acceptedMessage = acceptedMessage +" "+ receivedMessage[i]+":"+receiver;   
                    }

                    //Save info for notifying later
                    String infoPair = _sensorID+" "+receiver;
                    phonerLiteList.add(infoPair);
                }
                
                Subscriber._allowEvaluate = false; 
            }
        // execute when receive SUBSCRIBE
            if (Notifier._202Accepted == true) {
                new Notifier().processSubscribe(acceptedMessage);
                Notifier._202Accepted = false;
                dialogList.add(Notifier.dialog);                
            }    
            System.out.println("End configuration phase: Y/N?");
        } while (ChatClient.readCommand().equals("N"));
        
        // Start execution phase
        while(true){
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            
            while(Subscriber._allowCompare == true){
                String[] messageParts = Subscriber.message.split(":");
                String _sensorID = messageParts[0];
                
                for (int i=0; i < phonerLiteList.size(); i++){
                    String[] infoPair = phonerLiteList.get(i).split(" ");
                    if (infoPair[0].equals(_sensorID)){
                        // sensor:100:service1@192.168.50.11
                        notifyMessage = _sensorID+":"+messageParts[1]+":"+infoPair[1];
                    }
                }

                for (int i=0; i < dialogList.size();i++){
                    Dialog dialog = dialogList.get(i);
                    new Notifier().sendNotify(dialog, notifyMessage);
                }
                Subscriber._allowCompare = false;
            }
        }
    }
    
    private void executeCoAP() throws IOException, ServiceException {
        subsIPList = new ArrayList<String>();
        phonerLiteList = new ArrayList<>();
        _configurationDone = false;
        new Resource().init();
        
        for (int i=0; i < notifierList.size();i++){
            _commandParts = notifierList.get(i);
            
            String receiverPermURI = _commandParts;
            System.out.println("Looking for receiverPermURI: "+receiverPermURI+"\n");
            String receiverTempURI = _chordManager.lookup(receiverPermURI);
            if (receiverTempURI == null)
                System.out.println("The receiver does NOT exist! or wrong permURI, {e.g: Debian05@TKLAB.de} ");
            else if (receiverTempURI.equals(_username))
                System.out.println("You can NOT send a message to yourself.");
            else {
                String message = NodeProperties.userName+"@"+NodeProperties.p2pIP;
                new PutClient().init(receiverTempURI, message);
            }
        }
        
        do {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            // execute when receive response from SUBSCRIBE    
            if(PutClient._allowEvaluate&&!_configurationDone){
                String[] receivedMessage = PutClient.content.split(" ");
                for (int i=0; i< receivedMessage.length; i++){
                    String[] messageParts = receivedMessage[i].split(":");
                    String _sensorID = messageParts[0];
                    String _sensorFormat = messageParts[1];
                
                    if (_sensorFormat.equals("Boolean")){
                        System.out.println("Input Phonerlite Receiver URI for water detection in form abc@192.168.50.11: ");
                        receiver =  new ChatClient().readCommand();
                    } else if (_sensorFormat.equals("String")) {
                        System.out.println("Input Phonerlite Receiver URI for water temperature in form abc@192.168.50.11: ");
                        receiver =  new ChatClient().readCommand();                        
                    }
                    
                    if (acceptedMessage == null){
                        acceptedMessage = receivedMessage[i]+":"+receiver;                            
                    } else {
                        acceptedMessage = acceptedMessage +" "+ receivedMessage[i]+":"+receiver;   
                    }
                
                    //Save info for notifying later
                    String infoPair = _sensorID+" "+receiver;
                    phonerLiteList.add(infoPair);
                }
       
                PutClient._allowEvaluate = false;
            }
            // execute when receive SUBSCRIBE
            if (Resource._PUTreceived&&!_configurationDone) {  
                subsIPList.add(Resource.message);
                new Resource().sendAccepted(acceptedMessage);                 
                Resource._PUTreceived = false;
            }        
        System.out.println("End configuration phase: Y/N?");
        } while (ChatClient.readCommand().equals("N"));        
        _configurationDone = true;
        
        //Start execution phase
        while(true){
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            while(Resource._PUTreceived == true){
                String[] messageParts = Resource.message.split(":");
                String _sensorID = messageParts[0];
                for (int i=0; i < phonerLiteList.size();i++){
                    String[] infoPair = phonerLiteList.get(i).split(" ");
                    if (infoPair[0].equals(_sensorID)){
                        // sensor:100:service1@192.168.50.11
                        notifyMessage = _sensorID+":"+messageParts[1]+":"+infoPair[1];
                    }
                }

                new Resource().sendOK(null);
                for (int i=0; i < subsIPList.size();i++){
                    String subsIP = subsIPList.get(i);
                    new PutClient().init(subsIP, notifyMessage);               
                }
                Resource._PUTreceived = false;
            }
        }
    }
    
    private void executeMQTT() throws ServiceException {
        // initialize subscriber
        for (int i=0; i < notifierList.size();i++){
            _commandParts = notifierList.get(i);
            
            String receiverPermURI = _commandParts;
            System.out.println("Looking for receiverPermURI: "+receiverPermURI+"\n");
            String receiverTempURI = _chordManager.lookup(receiverPermURI);
            if (receiverTempURI == null)
                System.out.println("The receiver does NOT exist! or wrong permURI, {e.g: Debian05@TKLAB.de} ");
            else if (receiverTempURI.equals(_username))
                System.out.println("You can NOT send a message to yourself.");
            else {
                new SubsClient().runClient(receiverTempURI);
            }
        }
        
        // initialize broker
        try {           
            new Broker().init();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        // start execution phase
        while(true){
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            
            while(SubsClient._onPublish == true){
                String[] messageParts = SubsClient.receivedMessage.split(":");
                String _sensorID = messageParts[0];
                String _sensorFormat = messageParts[1];
                String _sensorValue = messageParts[2];
                
                if (_sensorFormat.equals("Boolean")){
                        System.out.println("Input Phonerlite Receiver URI for water detection in form abc@192.168.50.11: ");
                        receiver =  new ChatClient().readCommand();
                    } else if (_sensorFormat.equals("String")) {
                        System.out.println("Input Phonerlite Receiver URI for water temperature in form abc@192.168.50.11: ");
                        receiver =  new ChatClient().readCommand();                        
                    }
                
                Broker.payload = _sensorID+":"+_sensorValue+":"+receiver;
                try {
                // Broker.payload in form: sensor:500:service1@192.168.50.11
                    new Broker().selfPublish("/"+NodeProperties.userName, false, 2);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                
                SubsClient._onPublish = false;
            }
        }
    }
}
