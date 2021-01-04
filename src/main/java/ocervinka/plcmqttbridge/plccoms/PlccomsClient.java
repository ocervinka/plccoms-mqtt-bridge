package ocervinka.plcmqttbridge.plccoms;

import ocervinka.plcmqttbridge.telnet.TelnetClient;
import ocervinka.plcmqttbridge.telnet.TelnetClientListener;
import ocervinka.plcmqttbridge.VarMapping;
import ocervinka.plcmqttbridge.config.VarMappingConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlccomsClient {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pattern LIST_CMD_PATTERN = Pattern.compile("(.+),(.+)");

    private TelnetClient telnetClient;

    private final Function<Collection<PlccomsVar>, Collection<String>> listConsumer;
    private final Consumer<PlccomsDiff> diffConsumer;
    private final Map<String, VarMapping> varMappingsByVariable;

    private final Collection<PlccomsVar> vars = new ArrayList<>();


    public PlccomsClient(Function<Collection<PlccomsVar>, Collection<String>> listConsumer, Consumer<PlccomsDiff> diffConsumer) {
        this.listConsumer = listConsumer;
        this.diffConsumer = diffConsumer;
        this.varMappingsByVariable = varMappingsByVariable;
    }

    public final void connect(PlccomsConfig config) {
        telnetClient = new TelnetClient(config.host, config.port, new TelnetClientListener() {

            @Override
            public void onConnect(TelnetClient tc, String message) {
                LOGGER.info("Connected to PLCComS: " + message);
                //tc.write("GETINFO:");
                //tc.write("GETINFO:version");
                //tc.write("GETINFO:version_epsnet");
                //tc.write("GETINFO:version_ini");
                //tc.write("GETINFO:version_plc");
                //tc.write("GETINFO:ipaddr_plc");
                //tc.write("GETINFO:pubfile");
                //tc.write("GETINFO:network");

                tc.write("LIST:"); // list all variables when connected
            }

            @Override
            public void onLineRead(TelnetClient tc, String line) {
                String[] splitLine = line.trim().toUpperCase().split(":", -1);
                if (splitLine.length != 2) {
                    LOGGER.warn("Invalid command received (no single colon): {}", line);
                    return;
                }

                String cmd = splitLine[0];
                String args = splitLine[1].trim();

                if ("LIST".equals(cmd)) {
                    if (!args.isBlank()) {
                        Matcher matcher = LIST_CMD_PATTERN.matcher(args);
                        if (matcher.matches()) {
                            vars.add(new PlccomsVar(matcher.group(1), matcher.group(2)));
                        } else {
                            LOGGER.error("LIST args \"{}\" did not match {}", args, LIST_CMD_PATTERN);
                        }
                    } else { // last line of LIST command has no arguments
                        listConsumer.apply(vars);
                        Collection<String> varNamesToSubscribe = listConsumer.apply(vars);
                        VarMapping varMapping;
                        String delta;
                        for (String varName : varNamesToSubscribe) {
                            varMapping = varMappingsByVariable.get(varName);
                            delta = varMapping.config.stateTopicDelta == null ? "" : varMapping.config.stateTopicDelta.toPattern();
                            tc.write("EN:" + varName + " " + delta); // subscribe to changes
                            tc.write("GET:" + varName); // request initial value
                            LOGGER.trace("Enabled and GET: " + varName + " " + delta);
                        }
                    }

                } else if ("DIFF".equals(cmd) || "GET".equals(cmd)) {
                    try {
                        String[] diffArgs = args.split(",");
                        if (diffArgs.length != 2) {
                            LOGGER.info("DIFF/GET command must have two arguments: {}", args);
                            return;
                        }
                        diffConsumer.accept(new PlccomsDiff(diffArgs[0], diffArgs[1]));
                        LOGGER.trace("Received PLCComS: " +diffArgs[0]+" : "+diffArgs[1]);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to process DIFF/GET command: {}", args, e);
                    }
                } else {
                    LOGGER.info("Unexpected command \"{}\" received: ", line);
                }
            }

            @Override
            public void onError(TelnetClient tc, String message, Exception e) {
                LOGGER.info("PLCComS connection error", e);
            }

            @Override
            public void onDisconnect(TelnetClient tc) {
                LOGGER.info("Disconnected from PLCComS");
            }
        });
    }

    public void setVar(String name, Object value) {
        String cmd = "SET:" + name + "," + value.toString();
        telnetClient.write(cmd);
    }

    public void close() {
        telnetClient.close();
    }

}
