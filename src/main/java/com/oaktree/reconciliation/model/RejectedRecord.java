package com.oaktree.reconciliation.model;

public class RejectedRecord {

    private final Broker broker;
    private final String tradeId;
    private final String reason;

    public RejectedRecord(Broker broker, String tradeId, String reason) {
        this.broker = broker;
        this.tradeId = tradeId;
        this.reason = reason;
    }

    public Broker getBroker() {
        return broker;
    }

    public String getTradeId() {
        return tradeId;
    }

    public String getReason() {
        return reason;
    }
}
