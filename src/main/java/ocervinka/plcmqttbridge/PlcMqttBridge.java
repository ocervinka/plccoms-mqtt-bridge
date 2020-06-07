package ocervinka.plcmqttbridge;

import ocervinka.plcmqttbridge.config.Config;
import ocervinka.plcmqttbridge.config.VarMappingConfig;
import ocervinka.plcmqttbridge.mqtt.Mqtt;
import ocervinka.plcmqttbridge.plccoms.PlccomsClient;
import ocervinka.plcmqttbridge.plccoms.PlccomsDiff;
import ocervinka.plcmqttbridge.plccoms.PlccomsVar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlcMqttBridge {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DEFAULT_CONFIG = "/etc/plccoms-mqtt-bridge/config.yaml";

    private final Config config;
    private final Mqtt mqttClient;
    private final PlccomsClient plccomsClient;

    private final Map<String, VarMapping> varMappingsByTopic = new HashMap<>();
    private final Map<String, VarMapping> varMappingsByVariable = new HashMap<>();


    public static void main(String[] args) throws MqttException, IOException {
        String configFile = args.length == 0 ? DEFAULT_CONFIG : args[0];
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        Config config = objectMapper.readValue(new FileReader(configFile), Config.class);
        PlcMqttBridge plcMqttBridge = new PlcMqttBridge(config);
        plcMqttBridge.connect();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down plccoms-mqtt-bridge...");
            try {
                plcMqttBridge.close();
            } catch (MqttException e) {
                LOGGER.error("Failed to shut down pclcoms-mqtt-bridge", e);
            }
            LOGGER.info("plccoms-mqtt-bridge shut down");
        }, "ShutdownHookThread"));
    }

    public PlcMqttBridge(Config config) {
        this.config = config;
        this.mqttClient = new Mqtt();
        this.plccomsClient = new PlccomsClient(this::onList, this::onDiff);
    }

    private void connect() throws MqttException {
        mqttClient.connect(config.mqtt);
        plccomsClient.connect(config.plccoms);
    }

    private void close() throws MqttException {
        mqttClient.close();
        plccomsClient.close();
    }

    private Collection<String> onList(Collection<PlccomsVar> listedVars) {
        int blacklistedCount = 0;
        Map<String, Integer> blacklistedCountByPattern = new HashMap<>();
        List<String> unmappedVars = new ArrayList<>();

        for_each_label:
        for (PlccomsVar var : listedVars) {
            for (Pattern pattern : config.varBlacklist) {
                Matcher matcher = pattern.matcher(var.name);
                if (matcher.matches()) {
                    blacklistedCount++;
                    blacklistedCountByPattern.compute(pattern.pattern(), (k,v) -> v == null ? 1 : (v + 1));
                    continue for_each_label;
                }
            }

            for (VarMappingConfig config : config.varMappings) {
                Matcher matcher = config.varPattern.matcher(var.name);
                if (matcher.matches()) {
                    String topicName = config.stateTopic.format(getGroups(matcher));
                    varMappingsByVariable.put(var.name, new VarMapping(config, topicName));
                    LOGGER.info("State topic mapped:   {} {} -> {}", var.name, var.type, topicName);
                    if (config.cmdTopic != null) {
                        topicName = config.cmdTopic.format(getGroups(matcher));
                        varMappingsByTopic.put(topicName, new VarMapping(config, var.name));
                        LOGGER.info("Command topic mapped: {} {} <- {}", var.name, var.type, topicName);
                    }
                    continue for_each_label;
                }
            }

            unmappedVars.add(var.name + " " + var.type);
        }

        LOGGER.info("Total number of variables listed by PLCComS: {}", listedVars.size());
        LOGGER.info("Blacklisted variables: {}", blacklistedCount);
        for (Map.Entry<String, Integer> entry : blacklistedCountByPattern.entrySet()) {
            LOGGER.info("  {}: {}", entry.getKey(), entry.getValue());
        }
        LOGGER.info("Variables available for mapping: {}", listedVars.size() - blacklistedCount);
        LOGGER.info("Variables mapped to state topics: {}", varMappingsByVariable.size());
        LOGGER.info("Variables mapped from command topics: {}", varMappingsByTopic.size());
        LOGGER.info("Unmapped variables: {}", unmappedVars.size());
        for (String unmappedVar : unmappedVars) {
            LOGGER.info("  {}", unmappedVar);
        }

        try {
            mqttClient.subscribe(varMappingsByTopic.keySet(), (topic, message) -> {
                VarMapping varMapping = varMappingsByTopic.get(topic);
                String inputValue = new String(message.getPayload());
                String convertedValue = varMapping.config.cmdFunction.apply(inputValue);
                LOGGER.log(varMapping.config.logLevel, "MQTT->PLC: {},{} -> {},{}",
                        topic, inputValue, varMapping.destination, convertedValue);
                plccomsClient.setVar(varMapping.destination, convertedValue);
            });
        } catch (MqttException e) {
            LOGGER.error("Failed to subscribe to topic(s)", e);
        }

        return new ArrayList<>(varMappingsByVariable.keySet());
    }

    private void onDiff(PlccomsDiff diff) {
        VarMapping varMapping = varMappingsByVariable.get(diff.name);
        if (varMapping == null) {
            LOGGER.warn("Received unexpected variable from PLC: {}", diff.name);
            return;
        }

        String convertedValue;
        try {
            convertedValue = varMapping.config.stateFunction.apply(diff.value);
            LOGGER.log(varMapping.config.logLevel, "PLC->MQTT: {},{} -> {},{}",
                    diff.name, diff.value, varMapping.destination, convertedValue);
        } catch (Exception e) {
            LOGGER.error("PLC->MQTT: {},{} -> {},{}",
                    diff.name, diff.value, varMapping.destination, e.getMessage());
            return;
        }

        try {
            mqttClient.publish(varMapping.destination, convertedValue);
        } catch (Exception e) {
            LOGGER.error("Failed to publish to {}", varMapping.destination, e);
        }
    }

    private static String[] getGroups(Matcher matcher) {
        String[] groups = new String[matcher.groupCount() + 1];
        for (int i = 0; i < groups.length; i++) {
            groups[i] = matcher.group(i).toLowerCase();
        }

        return groups;
    }
}
