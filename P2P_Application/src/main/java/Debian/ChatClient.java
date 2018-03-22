package main.java.Debian;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.uniba.wiai.lspi.chord.service.ServiceException;
import java.util.ArrayList;
import java.util.List;

import main.java.Commands.ExitCommand;
import main.java.Commands.Service_1_Command;
import main.java.Commands.Service_2_Command;
import main.java.Commands.Service_3_Command;
import main.java.Commands.Service_4_Command;


public class ChatClient {
    private final static int MASTER_PORT = 8080;
    private String _username;
    public static boolean _isMaster;
    public static String protocol;
    

    public static void main(String[] args) throws ServiceException {
        _isMaster = false;
        System.out.println("Insert to start:");
        String input;
        input = readCommand();

        new ChatClient().start(input);
        System.out.println("End of main");
    }

    private void start(String initialcommand) throws ServiceException {
        if (wrongInitialize(initialcommand))
            printInstruction();
        
        // retrieve IP address
        String IP = new IP().getIP("eth0");
        NodeProperties.p2pIP = IP;

        System.out.println("Insert username:");
        _username = readCommand();
        NodeProperties.setUri(_username);
        System.out.println("\n"+"P2P IP: " + NodeProperties.p2pIP);
        System.out.println("Permanent Uri: " + NodeProperties.permUri);
        System.out.println("Temporary Uri: " + NodeProperties.tempUri +"\n");
        
        // set _isMaster or not
        setMasterStatus(initialcommand);
        
        // create network or joining network
        PresenceService chordManager = new ChordOverlayNetworkManager(_isMaster, NodeProperties.p2pIP, MASTER_PORT);
        
        // insert contact data
        if (!(_isMaster)){
            assignKeyAndData(chordManager);
        }
        else while(true) {}; // keep master running for being bootstrap address
        
        // insert communicate protocol
        try {
            printCommand2();
            System.out.println();
            System.out.print("$ ");            
            protocol = readCommand();            
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        
        
        // insert command
        try {
            printCommands1();
            while (true) {
                System.out.println();
                System.out.print("$ ");
                String command = readCommand();

                if (command.equals("3")){
                    System.out.println(_username+" is set as service 3.");
                    List<String> notifierList = new ArrayList<>();
                    
                    System.out.println("Subscribe to: [receiverPermURI]");
                    String receiverPermURI = readCommand();
                    notifierList.add(receiverPermURI);                    
                    System.out.println("Done subscription: Y/N?");
                    
                    while(readCommand().equals("N")) {                        
                        System.out.println("Subscribe to: [receiverPermURI]");
                        receiverPermURI = readCommand();
                        for (int i=0; i < notifierList.size();i++){
                            if (!notifierList.get(i).equals(receiverPermURI)){
                                notifierList.add(receiverPermURI);
                            }
                            else {
                                System.out.println("Duplication in subscription list.");
                            }
                        }
                        System.out.println("Done subscription: Y/N?");
                    }
                    new Service_3_Command(notifierList, chordManager, NodeProperties.tempUri).execute();}
                
                else if (command.equals("1")){
                    System.out.println(_username+" is set as service 1.");
                    new Service_1_Command(chordManager).execute();}
                
                else if (command.equals("2")){
                    System.out.println(_username+" is set as service 2.");
                    List<String> notifierList = new ArrayList<>();
                    
                    System.out.println("Subscribe to: [receiverPermURI]");
                    String receiverPermURI = readCommand();
                    notifierList.add(receiverPermURI);                    
                    System.out.println("Done subscription: Y/N?");
                    
                    while(readCommand().equals("N")) {                        
                        System.out.println("Subscribe to: [receiverPermURI]");
                        receiverPermURI = readCommand();
                        for (int i=0; i < notifierList.size();i++){
                            if (!notifierList.get(i).equals(receiverPermURI)){
                                notifierList.add(receiverPermURI);
                            }
                            else {
                                System.out.println("Duplication in subscription list.");
                            }
                        }
                        System.out.println("Done subscription: Y/N?");
                    }                    
                    new Service_2_Command(notifierList, chordManager, NodeProperties.tempUri).execute();
                }
                
                else if (command.equals("4")){
                    System.out.println(_username+" is set as service 4.");
                    List<String> notifierList = new ArrayList<>();
                    
                    System.out.println("Subscribe to: [receiverPermURI]");
                    String receiverPermURI = readCommand();
                    notifierList.add(receiverPermURI);                    
                    System.out.println("Done subscription: Y/N?");
                    
                    while(readCommand().equals("N")) {                        
                        System.out.println("Subscribe to: [receiverPermURI]");
                        receiverPermURI = readCommand();
                        for (int i=0; i < notifierList.size();i++){
                            if (!notifierList.get(i).equals(receiverPermURI)){
                                notifierList.add(receiverPermURI);
                            }
                            else {
                                System.out.println("Duplication in subscription list.");
                            }
                        }
                        System.out.println("Done subscription: Y/N?");
                    }  
                    new Service_4_Command(notifierList, chordManager, NodeProperties.tempUri).execute();
                }
                
                else if (command.equals("exit")){
                    new ExitCommand(chordManager).execute();                            
                }
                else {
                    System.out.println("Wrong Command, Retry!");}
                }
        } catch (Exception exception) {
                exception.printStackTrace();
        }
    }

    private void assignKeyAndData(PresenceService chordManager) throws ServiceException {
        if (!chordManager.register()) {
                System.err.println("The name '" + _username + "' is already taken!");
                System.exit(1);
        }	
    }

    private boolean wrongInitialize(String command) {
        return !(command.equals("master") || command.equals("client"));
    }

    private static void printInstruction() {
        System.err.println("Usage:\n" 
                        + "Input master to start a master node\n"
                        + "Input client to start a client node\n");
        System.exit(1);
    }

    private void printCommands1() {
        System.out.println("All Commands: ");        
        System.out.println("* 1: set peer as service 1");
        System.out.println("* 2: set peer as service 2");      
        System.out.println("* 4: set peer as service 4");
        System.out.println("* 3: set peer as service 3");
        System.out.println("* exit: to exit network");
    }
    
    private void printCommand2() {
        System.out.println("All protocols: ");
        System.out.println("* 1: SIP");
        System.out.println("* 2: CoAP");
        System.out.println("* 3: MQTT");
    }

    public static String readCommand() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            String input = bufferedReader.readLine();
            while (input.isEmpty())
                    input = bufferedReader.readLine();
            return input;
        } catch (IOException e) {
            System.out.println("IO error trying to read command!");
            System.exit(1);
        }
        return "";
    }

    private void setMasterStatus(String initialcommand) {
        if (initialcommand.equals("master")) {
        _isMaster = true;}
        else if (initialcommand.equals("client")) {
        _isMaster = false;}
    }
}
