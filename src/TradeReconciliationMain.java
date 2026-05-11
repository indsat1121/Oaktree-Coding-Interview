import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TradeReconciliationMain {

    public static void main(String[] args) throws IOException {
        // This is the main function that will be used to reconcile the trades between the two CSV files.
        // Below lines load CSV files in the path.
        Path pathA = Paths.get(args.length > 0 ? args[0] : "src/Trade_A.csv");
        Path pathB = Paths.get(args.length > 1 ? args[1] : "src/Trade_B.csv");

        // Below lines parse the CSV files and return the LoadResult object.
        // LoadResult object contains the valid trades, rejected trades, total data rows, and trade IDs appearing in the file.
        TradeCsvParser.LoadResult loadA = TradeCsvParser.load(Broker.A, pathA);
        TradeCsvParser.LoadResult loadB = TradeCsvParser.load(Broker.B, pathB);

        // Below lines reconcile the trades between the two CSV files and return the ReconciliationResult object.
        // ReconciliationResult object contains the rejected trades, conflicts, unified trades, and summary.
        ReconciliationResult result = TradeReconciliationService.reconcile(loadA, loadB);
        // Below lines print the report of the reconciliation result.   
        printReport(result);
    }

    private static void printReport(ReconciliationResult result) {
       // Below lines get the rejected records from the ReconciliationResult object and sort them by broker and trade ID.
        List<RejectedRecord> rejected = new ArrayList<>(result.getRejectedRecords());
        rejected.sort(Comparator
                .comparing((RejectedRecord r) -> r.getBroker() == Broker.A ? 0 : 1)
                .thenComparing(RejectedRecord::getTrade_id));

        System.out.println("Rejected records:");
        for (RejectedRecord r : rejected) {
            System.out.println(r.getBroker().getLabel() + " | " + r.getTrade_id() + " | " + r.getReason());
        }
        // Below lines print the conflict report.
        System.out.println();
        System.out.println("Conflict report:");
        for (FieldConflict c : result.getConflicts()) {
            System.out.println(c.getTrade_id() + " | " + c.getFieldName()
                    + " | A: " + c.getValueA() + " | B: " + c.getValueB());
        }
        // Below lines print the unified trades.
        System.out.println();
        System.out.println("Unified trades: " + result.getUnifiedTrades().size() + " records");
        for (Trade_Data t : result.getUnifiedTrades()) {
            System.out.println(formatUnifiedLine(t));
        }
        // Below lines print the summary of the reconciliation result.
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

    private static String formatUnifiedLine(Trade_Data t) {
        return t.getTrade_id() + "," + t.getSymbol() + "," + t.getSide() + ","
                + TradeReconciliationService.formatQuantity(t.getQuantity()) + ","
                + TradeReconciliationService.formatPrice(t.getPrice()) + ","
                + t.getTrade_date() + "," + t.getSettlement_date() + "," + t.getAccount_id();
    }
}
