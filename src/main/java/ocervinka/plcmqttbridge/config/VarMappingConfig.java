package ocervinka.plcmqttbridge.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ocervinka.plcmqttbridge.functions.Noop;
import ocervinka.plcmqttbridge.functions.OnToOne;
import ocervinka.plcmqttbridge.functions.OneToOn;
import org.apache.logging.log4j.Level;

import java.text.MessageFormat;
import java.util.function.Function;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public class VarMappingConfig {
    private static final Function<String, String> NOOP = new Noop();
    private static final Function<String, String> ONE_TO_ON = new OneToOn();
    private static final Function<String, String> ON_TO_ONE = new OnToOne();

    public final Pattern varPattern;
    public final MessageFormat stateTopic;
    public final MessageFormat stateTopicDelta;
    public final Function<String, String> stateFunction;
    public final MessageFormat cmdTopic;
    public final Function<String, String> cmdFunction;
    public final Level logLevel;

    @JsonCreator
    public VarMappingConfig(
            @JsonProperty(value = "var", required = true) String var,
            @JsonProperty(value = "state-topic", required = true) String stateTopic,
            @JsonProperty("delta") String stateTopicDelta,
            @JsonProperty("state-function") String stateFunction,
            @JsonProperty("cmd-topic") String cmdTopic,
            @JsonProperty("cmd-function") String cmdFunction,
            @JsonProperty("log-level") String logLevel)
    {
        this.varPattern = Pattern.compile(var);
        this.stateTopic = new MessageFormat(stateTopic);
        this.stateTopicDelta = stateTopicDelta == null ? null : new MessageFormat(stateTopicDelta);
        this.stateFunction = getFunction(stateFunction);
        this.cmdTopic = cmdTopic == null ? null : new MessageFormat(cmdTopic);
        this.cmdFunction = getFunction(cmdFunction);
        this.logLevel = Level.toLevel(logLevel, Level.INFO);
    }

    public static Function<String, String> getFunction(String functionName) {
        if (functionName == null) {
            return NOOP;
        } else if ("OneToOn".equals(functionName)) {
            return ONE_TO_ON;
        } else if ("OnToOne".equals(functionName)) {
            return ON_TO_ONE;
        } else {
            throw new IllegalArgumentException("Unknown converter " + functionName);
        }
    }
}
