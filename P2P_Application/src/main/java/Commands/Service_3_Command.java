package main.java.Commands;

import java.io.IOException;
import main.java.SIP.Subscriber;

import de.uniba.wiai.lspi.chord.service.ServiceException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import main.java.CoAP.PutClient;
import main.java.CoAP.Resource;
import main.java.Debian.ChatClient;
import main.java.Debian.NodeProperties;
import main.java.Debian.PresenceService;
import main.java.MQTT.SubsClient;
import main.java.SIP.IMS;

public class Service_3_Command implements ChordCommand {
    private String _commandParts;
    private PresenceService _chordManager;
    private String _username;
    private static List<String> notifierList;

    public Service_3_Command(List<String> notifierList, PresenceService chordManager, String userURI) {
        this.notifierList = notifierList;
        _chordManager = chordManager;
        _username = userURI;
    }

    @Override
    public void execute() throws ServiceException, IOException {
        // SIP
        if (ChatClient.protocol.equals("1")){
            // initialize subscriber
            Subscriber.myPort = 5070;
            new Subscriber().init();
             for (int i=0; i < notifierList.size();i++){
                _commandParts = notifierList.get(i);
                subs();   
            }
             
            // initialize SIP message
            IMS.myPort = 5060;
            new IMS().init();
            
            // Start execution phase (this peer only receive in configuration phase)
            while(true){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                if(Subscriber._allowCompare == true){
                    String[] messageParts = Subscriber.message.split(":");
                    String tempURI = messageParts[2];
                    new IMS().sendMessage(tempURI, "Received sensor value: \""+messageParts[1]+"\" from sensorID: \""+messageParts[0]+"\"");
                    Subscriber._allowCompare = false;
                }
            } 
        }            
        //CoAP
        else if (ChatClient.protocol.equals("2")) {
            // initialize SIP message
            IMS.myPort = 5060;
            new IMS().init();
            
            // initialize resource
            new Resource().init();
            
            // intitialize PutClient
            for (int i=0; i < notifierList.size();i++){
                _commandParts = notifierList.get(i);
                subs();   
            }
            
            // Start execution phase (this peer only receive in configuration phase)
            while (true){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                if (Resource._PUTreceived) {
                    new Resource().sendOK(null);
                    
                    String[] messageParts = Resource.message.split(":");
                    String tempURI = messageParts[2];
                    new IMS().sendMessage(tempURI, "Received sensor value: \""+messageParts[1]+"\" from sensorID: \""+messageParts[0]+"\"");                    
                    Resource._PUTreceived = false;
                }
            }
        }
        //MQTT
        else if (ChatClient.protocol.equals("3")) {
            // initialize subscriber
            for (int i=0; i < notifierList.size();i++){
                _commandParts = notifierList.get(i);
                subs();   
            }
            
            // initilize 
            IMS.myPort = 5060;
            new IMS().init();
            
            // Start execution phase (this peer only receive in configuration phase)
            while (true){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                
                if(SubsClient._onPublish == true){
                    String[] messageParts = SubsClient.receivedMessage.split(":");
                    String tempURI = messageParts[2];
                    new IMS().sendMessage(tempURI, "Received sensor value: \""+messageParts[1]+"\" from sensorID: \""+messageParts[0]+"\""); 
                    SubsClient._onPublish = false;
                }
            }
        }
    }

    private void subs() throws IOException, ServiceException {
        String receiverPermURI = _commandParts;
        System.out.println("Looking for receiverPermURI: "+receiverPermURI+"\n");

        String receiverTempURI = _chordManager.lookup(receiverPermURI);
        if (receiverTempURI == null)
            System.out.println("The receiver does NOT exist! or wrong permURI, {e.g: Debian05@TKLAB.de} ");
        else if (receiverTempURI.equals(_username))
            System.out.println("You can NOT send a message to yourself.");
        else {
            if (ChatClient.protocol.equals("1"))    
                new Subscriber().sendSubscribe(receiverTempURI, receiverPermURI);
            else if (ChatClient.protocol.equals("2")){
                String message = NodeProperties.userName+"@"+NodeProperties.p2pIP;
                new PutClient().init(receiverTempURI, message);    
            }
            else if (ChatClient.protocol.equals("3")){
                new SubsClient().runClient(receiverTempURI);
            }
        }
    }
}
