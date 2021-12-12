package ocervinka.plcmqttbridge.plccoms;

public class PlccomsVar {
    public final String name;
    public final String type;
    public final Double delta;

    public PlccomsVar(String name, String type, Double delta) {
        this.name = name;
        this.type = type;
        this.delta = delta;
    }

    public PlccomsVar(String name, String type) {
        this(name, type, null);
    }

    public PlccomsVar(String name, Double delta) {
        this(name, null, delta);
    }
}
