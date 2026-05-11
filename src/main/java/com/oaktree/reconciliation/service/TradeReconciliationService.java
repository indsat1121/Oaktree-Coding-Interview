package com.oaktree.reconciliation.service;

import com.oaktree.reconciliation.config.ReconciliationConfig;
import com.oaktree.reconciliation.config.ReconciliationConfigLoader;
import com.oaktree.reconciliation.config.ReconciliationStrategyType;
import com.oaktree.reconciliation.io.TradeCsvParser;
import com.oaktree.reconciliation.model.ReconciliationResult;
import com.oaktree.reconciliation.strategy.ExactMatchStrategy;
import com.oaktree.reconciliation.strategy.PriorityBrokerStrategy;
import com.oaktree.reconciliation.strategy.ReconciliationStrategy;
import com.oaktree.reconciliation.strategy.ToleranceMatchStrategy;

import java.io.IOException;

/**
 * Facade for reconciliation: selects a {@link ReconciliationStrategy} from {@link ReconciliationConfig}.
 */
public final class TradeReconciliationService {

    private TradeReconciliationService() {
    }

    public static ReconciliationStrategy newStrategy(ReconciliationConfig config) {
        ReconciliationStrategyType type = config.getStrategyType();
        if (type == ReconciliationStrategyType.TOLERANCE) {
            return new ToleranceMatchStrategy(config);
        }
        if (type == ReconciliationStrategyType.PRIORITY_BROKER) {
            return new PriorityBrokerStrategy(config);
        }
        return new ExactMatchStrategy(config);
    }

    public static ReconciliationResult reconcile(TradeCsvParser.LoadResult loadA, TradeCsvParser.LoadResult loadB, ReconciliationConfig config) {
        return newStrategy(config).reconcile(loadA, loadB);
    }

    /**
     * Uses {@link ReconciliationConfigLoader#load()} (classpath {@code application.properties} + env), or falls back to {@link ReconciliationConfig#defaultConfig()} if loading fails.
     */
    public static ReconciliationResult reconcile(TradeCsvParser.LoadResult loadA, TradeCsvParser.LoadResult loadB) {
        try {
            return reconcile(loadA, loadB, ReconciliationConfigLoader.load());
        } catch (IOException e) {
            return reconcile(loadA, loadB, ReconciliationConfig.defaultConfig());
        }
    }
}
