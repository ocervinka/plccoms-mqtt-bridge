package ocervinka.plcmqttbridge.telnet;

public interface TelnetClientListener {

    void onConnect(TelnetClient tc, String message);
    void onLineRead(TelnetClient tc, String line);
    void onError(TelnetClient tc, String message, Exception e);
    void onDisconnect(TelnetClient tc);
}
