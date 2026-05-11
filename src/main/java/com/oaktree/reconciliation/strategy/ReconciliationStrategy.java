package com.oaktree.reconciliation.strategy;

import com.oaktree.reconciliation.io.TradeCsvParser;
import com.oaktree.reconciliation.model.ReconciliationResult;

/**
 * Pluggable reconciliation policy (matching rules, conflict detection, unified book).
 */
public interface ReconciliationStrategy {

    ReconciliationResult reconcile(TradeCsvParser.LoadResult loadA, TradeCsvParser.LoadResult loadB);
}
