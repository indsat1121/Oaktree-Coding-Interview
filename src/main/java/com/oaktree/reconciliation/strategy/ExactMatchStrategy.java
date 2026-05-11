package com.oaktree.reconciliation.strategy;

import com.oaktree.reconciliation.config.ReconciliationConfig;

import java.math.BigDecimal;

/**
 * Strict {@link BigDecimal} equality for price and quantity; matched unified row always uses Broker A (exercise default).
 */
public final class ExactMatchStrategy extends AbstractReconciliationStrategy {

    public ExactMatchStrategy(ReconciliationConfig config) {
        super(config);
    }

    @Override
    protected boolean usePrimaryBrokerForMatchedUnified() {
        return false;
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
