package com.oaktree.reconciliation.strategy;

import com.oaktree.reconciliation.config.ReconciliationConfig;

import java.math.BigDecimal;

/**
 * Same strict numeric matching as {@link ExactMatchStrategy}, but the unified row for <em>matched</em> trades follows
 * {@link ReconciliationConfig#getPrimaryBroker()} (A or B).
 */
public final class PriorityBrokerStrategy extends AbstractReconciliationStrategy {

    public PriorityBrokerStrategy(ReconciliationConfig config) {
        super(config);
    }

    @Override
    protected boolean usePrimaryBrokerForMatchedUnified() {
        return true;
    }

    @Override
    protected boolean pricesEqual(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }
}
