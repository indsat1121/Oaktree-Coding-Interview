package com.oaktree.reconciliation;

import com.oaktree.reconciliation.io.TradeCsvParser;
import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.FieldConflict;
import com.oaktree.reconciliation.model.ReconciliationResult;
import com.oaktree.reconciliation.model.ReconciliationSummary;
import com.oaktree.reconciliation.model.RejectedRecord;
import com.oaktree.reconciliation.model.TradeData;
import com.oaktree.reconciliation.service.TradeReconciliationService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TradeReconciliationMain {

    public static void main(String[] args) throws IOException {
        Path pathA = Paths.get(args.length > 0 ? args[0] : "files/Trade_A.csv");
        Path pathB = Paths.get(args.length > 1 ? args[1] : "files/Trade_B.csv");

        TradeCsvParser.LoadResult loadA = TradeCsvParser.load(Broker.A, pathA);
        TradeCsvParser.LoadResult loadB = TradeCsvParser.load(Broker.B, pathB);

        ReconciliationResult result = TradeReconciliationService.reconcile(loadA, loadB);
        printReport(result);
    }

    private static void printReport(ReconciliationResult result) {
        List<RejectedRecord> rejected = new ArrayList<>(result.getRejectedRecords());
        rejected.sort(Comparator
                .comparing((RejectedRecord r) -> r.getBroker() == Broker.A ? 0 : 1)
                .thenComparing(RejectedRecord::getTradeId));

        System.out.println("Rejected records:");
        for (RejectedRecord r : rejected) {
            System.out.println(r.getBroker().getLabel() + " | " + r.getTradeId() + " | " + r.getReason());
        }

        System.out.println();
        System.out.println("Conflict report:");
        for (FieldConflict c : result.getConflicts()) {
            System.out.println(c.getTradeId() + " | " + c.getFieldName()
                    + " | A: " + c.getValueA() + " | B: " + c.getValueB());
        }

        System.out.println();
        System.out.println("Unified trades: " + result.getUnifiedTrades().size() + " records");
        for (TradeData t : result.getUnifiedTrades()) {
            System.out.println(formatUnifiedLine(t));
        }

        ReconciliationSummary s = result.getSummary();
        System.out.println();
        System.out.println("Summary:");
        System.out.println("Broker A: " + s.getBrokerATotal() + " total, " + s.getBrokerARejected() + " rejected, "
                + s.getBrokerAValid() + " valid");
        System.out.println("Broker B: " + s.getBrokerBTotal() + " total, " + s.getBrokerBRejected() + " rejected, "
                + s.getBrokerBValid() + " valid");
        System.out.println("Matched: " + s.getMatchedCount());
        System.out.println("Conflicts: " + s.getConflictCount());
        System.out.println("Unmatched (A only): " + s.getUnmatchedAOnly());
        System.out.println("Unmatched (B only): " + s.getUnmatchedBOnly());
    }

    private static String formatUnifiedLine(TradeData t) {
        return t.getTradeId() + "," + t.getSymbol() + "," + t.getSide() + ","
                + TradeReconciliationService.formatQuantity(t.getQuantity()) + ","
                + TradeReconciliationService.formatPrice(t.getPrice()) + ","
                + t.getTradeDate() + "," + t.getSettlementDate() + "," + t.getAccountId();
    }
}
