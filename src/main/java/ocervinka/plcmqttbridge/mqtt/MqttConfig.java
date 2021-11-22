package ocervinka.plcmqttbridge.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class MqttConfig {
    private static final String DEFAULT_SCHEME = "tcp";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT_TCP = 1883;
    private static final int DEFAULT_PORT_TLS = 8883;

    public final String scheme;
    public final String host;
    public final int port;
    public final String username;
    public final String password;
    public final String clientId;

    @JsonCreator
    public MqttConfig(
            @JsonProperty("scheme") String scheme,
            @JsonProperty("host") String host,
            @JsonProperty("port") Integer port,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("clientId") String clientId)
    {
        this.scheme = scheme == null ? DEFAULT_SCHEME : scheme;
        this.host = host == null ? DEFAULT_HOST : host;
        this.port = port == null ? (this.scheme.equals(DEFAULT_SCHEME) ? DEFAULT_PORT_TCP : DEFAULT_PORT_TLS) : port;
        this.username = username == null ? null : username;
        this.password = password == null ? null : password;
        this.clientId = clientId == null ? UUID.randomUUID().toString() : clientId;
    }


    public String getUri() {
        return scheme + "://" + host + ":" + port;
    }
}
