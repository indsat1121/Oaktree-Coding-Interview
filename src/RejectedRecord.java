public class RejectedRecord {

    private final Broker broker;
    private final String trade_id;
    private final String reason;

    public RejectedRecord(Broker broker, String trade_id, String reason) {
        this.broker = broker;
        this.trade_id = trade_id;
        this.reason = reason;
    }

    public Broker getBroker() {
        return broker;
    }

    public String getTrade_id() {
        return trade_id;
    }

    public String getReason() {
        return reason;
    }
}
