package main.java.Commands;

import main.java.SIP.Notifier;
import main.java.CoAP.Resource;
import java.io.IOException;

import de.uniba.wiai.lspi.chord.service.ServiceException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sip.Dialog;
import javax.sip.DialogState;
import main.java.CoAP.PutClient;
import main.java.Debian.ChatClient;
import main.java.Debian.NodeProperties;
import main.java.Debian.PresenceService;
import main.java.MQTT.Broker;



public class Service_1_Command implements ChordCommand {
    private PresenceService _chordManager;
    public String subsIP;
    public static boolean _service1On = true;
    private static String acceptedMessage;
    private static String valueMessage;

    public Service_1_Command(PresenceService chordManager) {
            _chordManager = chordManager;
    }

    @Override
    public void execute() throws IOException, ServiceException {
        // SIP
        if (ChatClient.protocol.equals("1")){
            readValueFormat();
            executeSIP();    
        }
        // CoAP
        else if (ChatClient.protocol.equals("2")) {
            readValueFormat();
            executeCoAP();
        }
        // MQTT
        else if (ChatClient.protocol.equals("3")) {
            try {
                executeMQTT();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void executeSIP() throws SocketException {
        List<Dialog> dialogList = new ArrayList<Dialog>();
        Notifier.myPort = 5070;
        new Notifier().init();
        
        do {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }            
            if((Notifier._202Accepted && _service1On) == true){
                new Notifier().processSubscribe(acceptedMessage);
                Notifier._202Accepted = false;
                dialogList.add(Notifier.dialog);
            }
            System.out.println("End configuration phase: Y/N?");
        } while (ChatClient.readCommand().equals("N"));
        
        while(true){
            readInputValue();
            for (int i=0; i < dialogList.size();i++){
                Dialog dialog = dialogList.get(i);
                if (dialog.getState() != DialogState.TERMINATED) {
                   new Notifier().sendNotify(dialog, valueMessage); 
                }   
            } 
        }
    }
    
    private void executeCoAP() throws SocketException{
        List<String> subIPList = new ArrayList<String>();
        new Resource().init();
        
        do {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }                
            if ((Resource._PUTreceived && _service1On) == true){                  
                subsIP = Resource.message;
                subIPList.add(subsIP);
                
                new Resource().sendAccepted(acceptedMessage);
                Resource._PUTreceived = false;
            }
            System.out.println("End configuration phase: Y/N?");
        } while (ChatClient.readCommand().equals("N"));
        
        while(true){
            readInputValue();
            for (int i=0; i < subIPList.size();i++){
                String subsIP = subIPList.get(i);
                new PutClient().init(subsIP, valueMessage);
            }
        }
    }   
    
    private void executeMQTT() throws IOException, InterruptedException{
        new Broker().init();
        while(true){
            readInputValue();
            Broker.payload = valueMessage;
            try {
                // valueMessage in form: sensor:String:500
                new Broker().selfPublish("/"+NodeProperties.userName, false, 2);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void readValueFormat() {
        System.out.println("Enter value format: String or Boolean");
        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();
        acceptedMessage = NodeProperties.userName+":"+input;
    }

    private void readInputValue() throws SocketException {
        System.out.println("Input sensor value (For MQTT: [format]:[value]) :");
        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();
        valueMessage = NodeProperties.userName+":"+input; 
    }
}


