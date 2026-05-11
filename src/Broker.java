public enum Broker {
    A("Broker A"),
    B("Broker B");

    private final String label;

    Broker(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
