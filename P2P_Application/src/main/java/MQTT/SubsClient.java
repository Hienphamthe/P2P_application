package main.java.MQTT;

import main.java.Debian.NodeProperties;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class SubsClient implements MqttCallback {
    public static boolean _onPublish;
    public static String receivedMessage;

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost!");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message){
        System.out.println("-------------------------------------------------");
        System.out.println("| Topic:" + topic);
        System.out.println("| Message: " + new String(message.getPayload()));
        System.out.println("-------------------------------------------------");
        receivedMessage = new String(message.getPayload());
        _onPublish = true;
                    }
                    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
    
    MqttClient myClient;
    MqttConnectOptions connOpt;

//    public static void main(String[] args) {
//        SubsClient smc = new SubsClient();
//        smc.runClient();
//    }
    
    public void runClient(String tempIP) {
        String toUserPermName = tempIP.substring(0, tempIP.indexOf("@"));
        String toAddressTemp = tempIP.substring(tempIP.indexOf("@")+1);
        String BROKER_URL = "tcp://"+toAddressTemp+":1883";
        // setup MQTT Client
        String clientID = NodeProperties.userName;
        connOpt = new MqttConnectOptions();
        connOpt.setCleanSession(true);

        // Connect to Broker
        try {
            myClient = new MqttClient(BROKER_URL, clientID);
            myClient.setCallback(this);
            myClient.connect(connOpt);
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Connected to " + BROKER_URL);

        // setup topic
        String myTopic = "/"+toUserPermName;

        try {
            int subQoS = 1;
            myClient.subscribe(myTopic, subQoS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}