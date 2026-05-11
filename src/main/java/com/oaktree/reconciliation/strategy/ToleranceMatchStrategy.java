package com.oaktree.reconciliation.strategy;

import com.oaktree.reconciliation.config.ReconciliationConfig;

import java.math.BigDecimal;

/**
 * Price differences within {@link ReconciliationConfig#getPriceTolerance()} are not treated as conflicts;
 * unified matched row follows {@link ReconciliationConfig#getPrimaryBroker()}.
 */
public final class ToleranceMatchStrategy extends AbstractReconciliationStrategy {

    public ToleranceMatchStrategy(ReconciliationConfig config) {
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
        BigDecimal tol = config.getPriceTolerance();
        if (tol == null || tol.compareTo(BigDecimal.ZERO) <= 0) {
            return a.compareTo(b) == 0;
        }
        return a.subtract(b).abs().compareTo(tol) <= 0;
    }
}
