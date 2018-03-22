package main.java.Commands;

import java.io.IOException;
import main.java.SIP.Subscriber;
import main.java.CoAP.PutClient;

import de.uniba.wiai.lspi.chord.service.ServiceException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sip.Dialog;
import static main.java.CoAP.PutClient.content;
import main.java.CoAP.Resource;
import main.java.Debian.ChatClient;
import main.java.Debian.NodeProperties;
import main.java.Debian.PresenceService;
import main.java.MQTT.Broker;
import main.java.MQTT.SubsClient;
import main.java.SIP.Notifier;


public class Service_2_Command implements ChordCommand {
    private String _commandParts;
    private PresenceService _chordManager;
    private String _username;

    private static String compare;
    private static String acceptedMessage;
    
    private static List<Dialog> dialogList;
    private static List<String> notifierList;
    private static List<String> sensorList;
    private static List<String> subsIPList;
    
    private static boolean _configurationDone;

    public Service_2_Command(List<String> notifierList, PresenceService chordManager,String userURI) {
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
        sensorList = new ArrayList<>();
        Subscriber.myPort = 5070;
        Notifier.myPort = 5060;           
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
                sensorList.add(Subscriber.message);
                Subscriber._allowEvaluate = false; 
            }
        // execute when receive SUBSCRIBE
            if (Notifier._202Accepted == true) {
                acceptedMessage = sensorList.get(0);
                if (sensorList.size() > 1){
                    for (int i=1; i < sensorList.size(); i++){
                        acceptedMessage = acceptedMessage+" "+sensorList.get(i);
                    }
                }
                new Notifier().processSubscribe(acceptedMessage);
                Notifier._202Accepted = false;
                dialogList.add(Notifier.dialog);                
            }    
            System.out.println("End configuration phase: Y/N?");
        } while (ChatClient.readCommand().equals("N"));
        
        // start execution phase
        while(true){
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            
            while(Subscriber._allowCompare == true){
                String[] messageParts = Subscriber.message.split(":");
                String _sensorID = messageParts[0];
                String _sensorValue = messageParts[1];
                compareValue(_sensorID, _sensorValue);
                Subscriber._allowCompare = false;
            }
        }
    }
    
    private void executeCoAP() throws IOException, ServiceException {
        subsIPList = new ArrayList<String>();
        sensorList = new ArrayList<>();
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
            // execute when receive response from PUT SUBSCRIBE   
            if(PutClient._allowEvaluate&&!_configurationDone){
                sensorList.add(PutClient.content);
                PutClient._allowEvaluate = false;
            }
            
            // execute when receive SUBSCRIBE
            if (Resource._PUTreceived&&!_configurationDone) {  
                acceptedMessage = sensorList.get(0);
                if (sensorList.size() > 1){
                    for (int i=1; i < sensorList.size(); i++){
                        acceptedMessage = acceptedMessage+" "+sensorList.get(i);
                    }
                }
            
                subsIPList.add(Resource.message);
                new Resource().sendAccepted(acceptedMessage);                 
                Resource._PUTreceived = false;
            }        
        System.out.println("End configuration phase: Y/N?");
        } while (ChatClient.readCommand().equals("N"));        
        _configurationDone = true;
        
        // start execution phase
        while(true){
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            
            while(Resource._PUTreceived == true){
                String[] messageParts = Resource.message.split(":");
                String _sensorID = messageParts[0];
                String _sensorValue = messageParts[1];

                new Resource().sendOK(null);
                compareValue(_sensorID, _sensorValue);
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
                
                compareValueMQTT(_sensorID, _sensorFormat, _sensorValue);
                SubsClient._onPublish = false;
            }
        }
    }
    
    private void compareValueMQTT (String _sensorID, String _sensorFormat, String _sensorValue) {
        switch (_sensorFormat){
            case "String":
                int _intValue = Integer.parseInt(_sensorValue);
                if (_intValue > 100) {
                    sendMessageMQTT(_sensorID, _sensorFormat, _sensorValue);
                }            
                break;
            case "Boolean":
                boolean _booValue = Boolean.parseBoolean(_sensorValue);
                if (_booValue == true) {
                    sendMessageMQTT(_sensorID, _sensorFormat, _sensorValue);
                }
                break;
            default:                     
                System.out.println("No case matched!");
        }
    }
    
    private void sendMessageMQTT(String _sensorID, String _sensorFormat, String _sensorValue) {
        Broker.payload = _sensorID+":"+_sensorFormat+":"+_sensorValue;
        try {
            // valueMessage in form: sensor:500
            new Broker().selfPublish("/"+NodeProperties.userName, false, 2);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    private void compareValue(String _sensorID, String _sensorValue) throws SocketException {
        for (int i=0; i < sensorList.size();i++){
            String[] infoPair = sensorList.get(i).split(":");
            if (infoPair[0].equals(_sensorID)){
                switch (infoPair[1]){
                    case "String":
                        int _intValue = Integer.parseInt(_sensorValue);
                        if (_intValue > 100) {
                            sendMessage(_sensorID, _sensorValue);
                        }
                    break;
                    case "Boolean":
                        boolean _booValue = Boolean.parseBoolean(_sensorValue);
                        if (_booValue == true) {
                            sendMessage(_sensorID, _sensorValue);
                        }
                        break; 
                    // Not in this scenario extension
                    case "Float":
                        float _floatValue = Float.parseFloat(_sensorValue);
                        int _floatCompare = Float.compare(5.5f,_floatValue);
                        if (_floatCompare > 0) {
                            sendMessage(_sensorID, _sensorValue);
                        }
                        break;
                    // Not in this scenario extension
                    case "Int":
                        int _intValue1 = Integer.parseInt(_sensorValue);
                        if (_intValue1 > 5) {
                            sendMessage(_sensorID, _sensorValue);
                        }
                        break;  
                    default:                     
                        System.out.println("No case matched!");
                }
            }
        }
    }

    private void sendMessage(String _sensorID, String _sensorValue) throws SocketException {
        String message;
        message = _sensorID+":"+_sensorValue;
        if (ChatClient.protocol.equals("1")){
            for (int i=0; i < dialogList.size();i++){
                Dialog dialog = dialogList.get(i);
                new Notifier().sendNotify(dialog, message);
            }
        }
        else if (ChatClient.protocol.equals("2")) {
            for (int i=0; i < subsIPList.size();i++){
                String subsIP = subsIPList.get(i);
                new PutClient().init(subsIP, message);               
            }
        }
    }
}
