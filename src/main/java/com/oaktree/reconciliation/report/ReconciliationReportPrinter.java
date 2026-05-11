package com.oaktree.reconciliation.report;

import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.FieldConflict;
import com.oaktree.reconciliation.model.ReconciliationResult;
import com.oaktree.reconciliation.model.ReconciliationSummary;
import com.oaktree.reconciliation.model.RejectedRecord;
import com.oaktree.reconciliation.model.TradeData;
import com.oaktree.reconciliation.util.TradeAmountFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Renders a {@link ReconciliationResult} as plain text (one consumer call per output line).
 */
public final class ReconciliationReportPrinter {

    private ReconciliationReportPrinter() {
    }

    public static void printLines(ReconciliationResult result, Consumer<String> out) {
        List<RejectedRecord> rejected = new ArrayList<>(result.getRejectedRecords());
        rejected.sort(Comparator
                .comparing((RejectedRecord r) -> r.getBroker() == Broker.A ? 0 : 1)
                .thenComparing(RejectedRecord::getTradeId));

        out.accept("Rejected records:");
        for (RejectedRecord r : rejected) {
            out.accept(r.getBroker().getLabel() + " | " + r.getTradeId() + " | " + r.getReason());
        }

        out.accept("");
        out.accept("Conflict report:");
        for (FieldConflict c : result.getConflicts()) {
            out.accept(c.getTradeId() + " | " + c.getFieldName()
                    + " | A: " + c.getValueA() + " | B: " + c.getValueB());
        }

        out.accept("");
        out.accept("Unified trades: " + result.getUnifiedTrades().size() + " records");
        for (TradeData t : result.getUnifiedTrades()) {
            out.accept(formatUnifiedCsvLine(t));
        }

        ReconciliationSummary s = result.getSummary();
        out.accept("");
        out.accept("Summary:");
        out.accept("Broker A: " + s.getBrokerATotal() + " total, " + s.getBrokerARejected() + " rejected, "
                + s.getBrokerAValid() + " valid");
        out.accept("Broker B: " + s.getBrokerBTotal() + " total, " + s.getBrokerBRejected() + " rejected, "
                + s.getBrokerBValid() + " valid");
        out.accept("Matched: " + s.getMatchedCount());
        out.accept("Conflicts: " + s.getConflictCount());
        out.accept("Unmatched (A only): " + s.getUnmatchedAOnly());
        out.accept("Unmatched (B only): " + s.getUnmatchedBOnly());
    }

    public static String formatUnifiedCsvLine(TradeData t) {
        return t.getTradeId() + "," + t.getSymbol() + "," + t.getSide() + ","
                + TradeAmountFormatter.formatQuantity(t.getQuantity()) + ","
                + TradeAmountFormatter.formatPrice(t.getPrice()) + ","
                + t.getTradeDate() + "," + t.getSettlementDate() + "," + t.getAccountId();
    }
}
