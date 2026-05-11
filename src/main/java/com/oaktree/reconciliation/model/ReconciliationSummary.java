package com.oaktree.reconciliation.model;

public class ReconciliationSummary {

    private final int brokerATotal;
    private final int brokerARejected;
    private final int brokerAValid;
    private final int brokerBTotal;
    private final int brokerBRejected;
    private final int brokerBValid;
    private final int matchedCount;
    private final int conflictCount;
    private final int unmatchedAOnly;
    private final int unmatchedBOnly;

    public ReconciliationSummary(
            int brokerATotal,
            int brokerARejected,
            int brokerAValid,
            int brokerBTotal,
            int brokerBRejected,
            int brokerBValid,
            int matchedCount,
            int conflictCount,
            int unmatchedAOnly,
            int unmatchedBOnly) {
        this.brokerATotal = brokerATotal;
        this.brokerARejected = brokerARejected;
        this.brokerAValid = brokerAValid;
        this.brokerBTotal = brokerBTotal;
        this.brokerBRejected = brokerBRejected;
        this.brokerBValid = brokerBValid;
        this.matchedCount = matchedCount;
        this.conflictCount = conflictCount;
        this.unmatchedAOnly = unmatchedAOnly;
        this.unmatchedBOnly = unmatchedBOnly;
    }

    public int getBrokerATotal() {
        return brokerATotal;
    }

    public int getBrokerARejected() {
        return brokerARejected;
    }

    public int getBrokerAValid() {
        return brokerAValid;
    }

    public int getBrokerBTotal() {
        return brokerBTotal;
    }

    public int getBrokerBRejected() {
        return brokerBRejected;
    }

    public int getBrokerBValid() {
        return brokerBValid;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public int getConflictCount() {
        return conflictCount;
    }

    public int getUnmatchedAOnly() {
        return unmatchedAOnly;
    }

    public int getUnmatchedBOnly() {
        return unmatchedBOnly;
    }
}
