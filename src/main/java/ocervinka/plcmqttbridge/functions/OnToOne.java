package ocervinka.plcmqttbridge.functions;

import java.util.function.Function;

public class OnToOne implements Function<String, String> {

    @Override
    public String apply(String input) {
        if ("OFF".equals(input)) {
            return "0";
        } else if ("ON".equals(input)){
            return "1";
        } else {
            throw new IllegalArgumentException("Failed to convert unknown value " + input);
        }
    }
}
