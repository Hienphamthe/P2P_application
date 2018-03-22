package main.java.Debian;

import java.io.IOException;
import java.io.Serializable;
//import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
//import java.net.UnknownHostException;
import java.util.Set;

import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
//import java.io.InputStream;
//import java.util.Properties;
import java.util.Scanner;

public class ChordOverlayNetworkManager implements PresenceService {
    private Chord _chord;

    public ChordOverlayNetworkManager(boolean isMaster, String localIP, int port) {
        super();
        PropertiesLoader.loadPropertyFile();
        if (isMaster)
            createChordNetwork(localIP, port);
        else {
            System.out.println("Insert bootstrapIP: ");
            String bootstrapIP = new ChatClient().readCommand();
            System.out.println("Insert bootstrapPort: ");
            Scanner scan = new Scanner(System.in);
            int bootstrapPort = scan.nextInt();
            System.out.println();
            joinChordNetwork(localIP, bootstrapIP, bootstrapPort);}
    }

    @Override
    public boolean register() throws ServiceException {
            StringKey key = new StringKey(NodeProperties.permUri);
            if (nodeAlreadyExists(key)){
            return false;}
            else {
            _chord.insert(key, NodeProperties.tempUri);
            System.out.println("Insert with key completed");
            return true;}
    }

    @Override
    public void unregister() throws ServiceException {
        StringKey key = new StringKey(NodeProperties.permUri);
        _chord.remove(key, NodeProperties.tempUri);
    }
    
    @Override
    public void leaveNetwork(){
        try {
            _chord.leave();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String lookup(String permURI) throws ServiceException {
        StringKey key = new StringKey(permURI);
        String tempURI = null;
        Set<Serializable> nodes = _chord.retrieve(key);
        if (!nodes.isEmpty()){
            for (Serializable i : nodes){
                tempURI = (String) i;
            }
        }
        return tempURI;
    }

    private void createChordNetwork(String localIP, int port) {
            String protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);

            // create current URL
            String localUrlString;
            URL localUrl = null;
            localUrlString = createUrl(protocol, localIP, port);

            try {
                    localUrl = new URL(localUrlString);
            } catch (MalformedURLException e) {
                    throw new RuntimeException("Incorrect Url: " + localUrlString, e);
            }

            _chord = new ChordImpl();
            try {
                    _chord.create(localUrl);
                    System.out.println("\nCreated network with first node's URL (BootstrapURL): "+localUrl+"\n");
            } catch (ServiceException exception) {
                    throw new RuntimeException("Could not create DHT ! ", exception);
            }
    }

    private void joinChordNetwork(String localIP, String bootstrapIP, int bootstrapPort) {
            String protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);

            // get available port for P2P communication (not SIP)
            int localPort;
            try {
                    localPort = getPort();
            } catch (IOException exception) {
                    throw new RuntimeException("Error while trying to find free port!",exception);
            }

            // create localURL
            String localUrlStr;
            URL localUrl = null;
            localUrlStr = createUrl(protocol, localIP, localPort);
            try {
                    localUrl = new URL(localUrlStr);
            } catch (MalformedURLException e) {
                    throw new RuntimeException("Incorrect Url: " + localUrlStr, e);
            }

            // create bootstrapURL from bootstrapIP and bootstrapPort
            String bootstrapUrlStr = createUrl(protocol, bootstrapIP, bootstrapPort);
            URL bootstrapUrl = null;
            try {
                    bootstrapUrl = new URL(bootstrapUrlStr);
            } catch (MalformedURLException e) {
                    throw new RuntimeException("Incorrect Url: " + bootstrapUrlStr, e);
            }

            _chord = new ChordImpl();
            try {
                    _chord.join(localUrl, bootstrapUrl);
                    System.out.println("\nNode with URL: " +localUrl+ " has join the network.");
            } catch (ServiceException e) {
                    throw new RuntimeException("Could not join DHT!", e);
            }
            
    }

    private boolean nodeAlreadyExists(StringKey key) throws ServiceException {
            return !_chord.retrieve(key).isEmpty();
    }

    private int getPort() throws IOException {
            ServerSocket server = new ServerSocket(0);
            int localPort = server.getLocalPort();
            server.close();
            return localPort;
    }

    private String createUrl(String protocol, String IP, int port) {
            return protocol + "://" + IP + ':' + port + '/';
    }
}
