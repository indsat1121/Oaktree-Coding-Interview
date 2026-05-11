package com.oaktree.reconciliation;

import com.oaktree.reconciliation.config.ReconciliationConfig;
import com.oaktree.reconciliation.config.ReconciliationConfigLoader;
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
        ReconciliationConfig config = ReconciliationConfigLoader.load();
        if (args.length >= 2) {
            config = config.withFeeds(Paths.get(args[0]), Paths.get(args[1]));
        }

        Path pathA = config.getFeedPathA();
        Path pathB = config.getFeedPathB();

        TradeCsvParser.LoadResult loadA = TradeCsvParser.load(Broker.A, pathA);
        TradeCsvParser.LoadResult loadB = TradeCsvParser.load(Broker.B, pathB);

        ReconciliationResult result = TradeReconciliationService.reconcile(loadA, loadB, config);
        ReconciliationReportPrinter.printLines(result, System.out::println, config);
    }
}
