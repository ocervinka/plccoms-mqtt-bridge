package ocervinka.plcmqttbridge.functions;

import java.util.function.Function;

public class OneToOn implements Function<String, String> {

    @Override
    public String apply(String input) {
        if ("0".equals(input)) {
            return "OFF";
        } else if ("1".equals(input)) {
            return "ON";
        } else {
            throw new IllegalArgumentException("Failed to convert unknown value " + input);
        }
    }
}
