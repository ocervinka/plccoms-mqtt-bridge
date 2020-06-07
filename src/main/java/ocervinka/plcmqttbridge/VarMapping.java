package ocervinka.plcmqttbridge;

import ocervinka.plcmqttbridge.config.VarMappingConfig;

public class VarMapping {

    public final VarMappingConfig config;
    public final String destination;

    public VarMapping(VarMappingConfig config, String destination) {
        this.config = config;
        this.destination = destination;
    }
}
