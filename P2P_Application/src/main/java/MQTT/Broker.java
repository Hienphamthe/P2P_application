package main.java.MQTT;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
 
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.proto.messages.AbstractMessage;
import io.moquette.proto.messages.AbstractMessage.QOSType;
import io.moquette.proto.messages.PublishMessage;
import io.moquette.server.Server;
import io.moquette.server.config.ClasspathConfig;
import io.moquette.server.config.IConfig;
import java.nio.ByteBuffer;
 
public class Broker {
public static String payload;
private static Server mqttBroker;

    static class PublisherListener extends AbstractInterceptHandler {
        @Override
        public void onPublish(InterceptPublishMessage message) {
            payload = new String(message.getPayload().array());
            System.out.println("moquette mqtt broker message intercepted, topic: " + message.getTopicName()
                + ", content: " + payload);
        }
        
        @Override
        public void onSubscribe(InterceptSubscribeMessage message) {
            message.getClientID();
            message.getRequestedQos();
            message.getTopicFilter();
            System.out.println("Received subscription");
        }
    }
    
    public void init() throws IOException {
        payload = null;
        
        // Creating a MQTT Broker using Moquette
        final IConfig classPathConfig = new ClasspathConfig();

        mqttBroker = new Server();
        final List<? extends InterceptHandler> userHandlers = Arrays.asList(new PublisherListener());
        mqttBroker.startServer(classPathConfig, userHandlers);
        System.out.println("Moquette mqtt broker started");
    }
    
    public void selfPublish (String topic, Boolean _retain, int _qos) throws InterruptedException {
        PublishMessage message = new PublishMessage();
        message.setTopicName(topic);
        if (_retain){
            message.setRetainFlag(true);
        } else {
            message.setRetainFlag(false);
        }
        
        if (_qos==0){
            message.setQos(AbstractMessage.QOSType.MOST_ONE);
        } else if (_qos==1) {
            message.setQos(AbstractMessage.QOSType.LEAST_ONE);
        } else if (_qos==2) {
            message.setQos(AbstractMessage.QOSType.EXACTLY_ONCE);
        }
        if (payload != null){
            message.setPayload(ByteBuffer.wrap(payload.getBytes()));
        } else {
            message.setPayload(null);
        }
        mqttBroker.internalPublish(message);
        System.out.println("Selfpublished");
    
    }
}