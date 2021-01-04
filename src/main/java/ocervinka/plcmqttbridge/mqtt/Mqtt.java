package ocervinka.plcmqttbridge.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Collection;

public class Mqtt {

    private static final Logger LOGGER = LogManager.getLogger();

    private IMqttClient client;


    public void connect(MqttConfig config) throws MqttException {
        client = new MqttClient(config.getUri(), config.clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        if (config.username != "") options.setUserName(config.username);
        if (config.password != "") {
            String passw = config.password;
            char[] chPassw = passw.toCharArray();
            options.setPassword(chPassw);
        }
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        client.connect(options);
    }

    public void close() throws MqttException {
        if (client == null) {
            return;
        }

        client.disconnect();
        client.close();
    }

    public void publish(String topic, String value) throws Exception {
        if (client == null || !client.isConnected()) {
            return;
        }

        byte[] payload = value.getBytes();
        MqttMessage msg = new MqttMessage(payload);
        msg.setQos(0);
        msg.setRetained(true);
        client.publish(topic, msg);
    }

    public void subscribe(Collection<String> topicFilters, IMqttMessageListener listener) throws MqttException {
        for (String topicFilter : topicFilters) {
            LOGGER.info("Subscribing to topic {}", topicFilter);
            client.subscribe(topicFilter, listener);
        }
    }
}
