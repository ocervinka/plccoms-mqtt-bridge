package ocervinka.plcmqttbridge.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertArrayEquals;

public class MqttConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Test
    public void empty() throws IOException {
        var mqttConfig = MAPPER.readValue("{}", MqttConfig.class);
        Assert.assertNotNull(mqttConfig.clientId);
    }

    @Test
    public void withCredentials() throws IOException {
        var mqttConfig = MAPPER.readValue(resource("mqtt-config-with-credentials.json"), MqttConfig.class);
        Assert.assertEquals("tester", mqttConfig.username);
        assertArrayEquals("123456".toCharArray(), mqttConfig.password);
    }

    private static InputStream resource(String resourceFileName) {
        return MqttConfigTest.class.getClassLoader().getResourceAsStream(resourceFileName);
    }
}