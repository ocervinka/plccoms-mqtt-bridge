package ocervinka.plcmqttbridge.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ocervinka.plcmqttbridge.mqtt.MqttConfig;
import ocervinka.plcmqttbridge.plccoms.PlccomsConfig;

import java.util.regex.Pattern;

public class Config {
    public final PlccomsConfig plccoms;
    public final MqttConfig mqtt;
    public final Pattern[] varBlacklist;
    public final VarMappingConfig[] varMappings;

    @JsonCreator
    public Config(
            @JsonProperty("plccoms") PlccomsConfig plccoms,
            @JsonProperty("mqtt") MqttConfig mqtt,
            @JsonProperty("var-blacklist") String[] varBlacklist,
            @JsonProperty("var-mapping") VarMappingConfig[] varMappings)
    {
        this.plccoms = plccoms;
        this.mqtt = mqtt;
        this.varBlacklist = new Pattern[varBlacklist == null ? 0 : varBlacklist.length];
        for (int i = 0; i < this.varBlacklist.length; i++) {
            this.varBlacklist[i] = Pattern.compile(varBlacklist[i]);
        }
        this.varMappings = varMappings;
    }
}
