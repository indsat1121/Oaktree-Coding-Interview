package com.oaktree.reconciliation.strategy;

import com.oaktree.reconciliation.config.ReconciliationConfig;
import com.oaktree.reconciliation.io.TradeCsvParser;
import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.FieldConflict;
import com.oaktree.reconciliation.model.ReconciliationResult;
import com.oaktree.reconciliation.model.ReconciliationSummary;
import com.oaktree.reconciliation.model.RejectedRecord;
import com.oaktree.reconciliation.model.TradeData;
import com.oaktree.reconciliation.util.TradeAmountFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Shared reconciliation pipeline; subclasses define price equality and how the unified row is chosen for matched trades.
 */
abstract class AbstractReconciliationStrategy implements ReconciliationStrategy {

    protected final ReconciliationConfig config;

    protected AbstractReconciliationStrategy(ReconciliationConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public final ReconciliationResult reconcile(TradeCsvParser.LoadResult loadA, TradeCsvParser.LoadResult loadB) {
        Map<String, TradeData> mapA = loadA.validByTradeId();
        Map<String, TradeData> mapB = loadB.validByTradeId();

        Set<String> matchedIds = new HashSet<>(mapA.keySet());
        matchedIds.retainAll(mapB.keySet());

        List<FieldConflict> conflicts = new ArrayList<>();
        List<String> sortedMatched = new ArrayList<>(matchedIds);
        Collections.sort(sortedMatched);
        for (String id : sortedMatched) {
            conflicts.addAll(compareFields(id, mapA.get(id), mapB.get(id)));
        }

        Set<String> idsInFileA = loadA.getTradeIdsAppearingInFile();
        Set<String> idsInFileB = loadB.getTradeIdsAppearingInFile();

        Set<String> onlyA = new HashSet<>();
        for (String id : mapA.keySet()) {
            if (!idsInFileB.contains(id)) {
                onlyA.add(id);
            }
        }

        Set<String> onlyB = new HashSet<>();
        for (String id : mapB.keySet()) {
            if (!idsInFileA.contains(id)) {
                onlyB.add(id);
            }
        }

        List<TradeData> unified = new ArrayList<>();
        TreeMap<String, TradeData> unifiedById = new TreeMap<>();
        for (String id : matchedIds) {
            TradeData a = mapA.get(id);
            TradeData b = mapB.get(id);
            unifiedById.put(id, pickMatchedUnifiedRow(a, b));
        }
        for (String id : onlyA) {
            unifiedById.put(id, mapA.get(id));
        }
        for (String id : onlyB) {
            unifiedById.put(id, mapB.get(id));
        }
        unified.addAll(unifiedById.values());

        List<RejectedRecord> rejected = new ArrayList<>();
        rejected.addAll(loadA.getRejected());
        rejected.addAll(loadB.getRejected());

        ReconciliationSummary summary = new ReconciliationSummary(
                loadA.getTotalDataRows(),
                loadA.getRejected().size(),
                loadA.getValidTrades().size(),
                loadB.getTotalDataRows(),
                loadB.getRejected().size(),
                loadB.getValidTrades().size(),
                matchedIds.size(),
                conflicts.size(),
                onlyA.size(),
                onlyB.size());

        return new ReconciliationResult(rejected, conflicts, unified, summary);
    }

    /**
     * {@code true}: unified matched row follows {@link ReconciliationConfig#getPrimaryBroker()}.
     * {@code false}: legacy behaviour — always Broker A row for matched trades.
     */
    protected abstract boolean usePrimaryBrokerForMatchedUnified();

    /** Whether two prices are considered equal for conflict detection. */
    protected abstract boolean pricesEqual(BigDecimal a, BigDecimal b);

    protected TradeData pickMatchedUnifiedRow(TradeData rowA, TradeData rowB) {
        if (!usePrimaryBrokerForMatchedUnified()) {
            return rowA;
        }
        return config.getPrimaryBroker() == Broker.B ? rowB : rowA;
    }

    protected List<FieldConflict> compareFields(String tradeId, TradeData a, TradeData b) {
        List<FieldConflict> out = new ArrayList<>();
        if (!safeEquals(a.getSymbol(), b.getSymbol())) {
            out.add(new FieldConflict(tradeId, "symbol", a.getSymbol(), b.getSymbol()));
        }
        if (!safeEquals(a.getSide(), b.getSide())) {
            out.add(new FieldConflict(tradeId, "side", a.getSide(), b.getSide()));
        }
        if (!quantitiesEqual(a.getQuantity(), b.getQuantity())) {
            out.add(new FieldConflict(
                    tradeId,
                    "quantity",
                    TradeAmountFormatter.formatQuantity(a.getQuantity()),
                    TradeAmountFormatter.formatQuantity(b.getQuantity())));
        }
        if (!pricesEqual(a.getPrice(), b.getPrice())) {
            out.add(new FieldConflict(
                    tradeId,
                    "price",
                    TradeAmountFormatter.formatPrice(a.getPrice(), config.getPriceDisplayScale(), config.getPriceRoundingMode()),
                    TradeAmountFormatter.formatPrice(b.getPrice(), config.getPriceDisplayScale(), config.getPriceRoundingMode())));
        }
        if (!Objects.equals(a.getTradeDate(), b.getTradeDate())) {
            out.add(new FieldConflict(tradeId, "trade_date", String.valueOf(a.getTradeDate()), String.valueOf(b.getTradeDate())));
        }
        if (!Objects.equals(a.getSettlementDate(), b.getSettlementDate())) {
            out.add(new FieldConflict(tradeId, "settlement_date", String.valueOf(a.getSettlementDate()), String.valueOf(b.getSettlementDate())));
        }
        if (!safeEquals(a.getAccountId(), b.getAccountId())) {
            out.add(new FieldConflict(tradeId, "account_id", a.getAccountId(), b.getAccountId()));
        }
        return out;
    }

    private static boolean quantitiesEqual(BigDecimal x, BigDecimal y) {
        if (x == null && y == null) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.compareTo(y) == 0;
    }

    private static boolean safeEquals(String x, String y) {
        if (x == null && y == null) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }
}
