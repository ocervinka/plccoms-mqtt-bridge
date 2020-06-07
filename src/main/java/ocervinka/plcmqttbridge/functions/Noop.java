package ocervinka.plcmqttbridge.functions;

import java.util.function.Function;

public class Noop implements Function<String, String> {

    @Override
    public String apply(String input) {
        return input;
    }
}
