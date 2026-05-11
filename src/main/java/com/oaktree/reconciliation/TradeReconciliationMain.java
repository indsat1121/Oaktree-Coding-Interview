package com.oaktree.reconciliation;

import com.oaktree.reconciliation.io.TradeCsvParser;
import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.ReconciliationResult;
import com.oaktree.reconciliation.report.ReconciliationReportPrinter;
import com.oaktree.reconciliation.service.TradeReconciliationService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TradeReconciliationMain {

    public static void main(String[] args) throws IOException {
        Path pathA = Paths.get(args.length > 0 ? args[0] : "files/Trade_A.csv");
        Path pathB = Paths.get(args.length > 1 ? args[1] : "files/Trade_B.csv");

        TradeCsvParser.LoadResult loadA = TradeCsvParser.load(Broker.A, pathA);
        TradeCsvParser.LoadResult loadB = TradeCsvParser.load(Broker.B, pathB);

        ReconciliationResult result = TradeReconciliationService.reconcile(loadA, loadB);
        ReconciliationReportPrinter.printLines(result, System.out::println);
    }
}
