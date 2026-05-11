package com.oaktree.reconciliation.strategy;

import com.oaktree.reconciliation.config.ReconciliationConfig;
import com.oaktree.reconciliation.config.ReconciliationStrategyType;
import com.oaktree.reconciliation.io.TradeCsvParser;
import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.ReconciliationResult;
import com.oaktree.reconciliation.service.TradeReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationStrategyTest {

    private static final String HEADER =
            "trade_id,symbol,side,quantity,price,trade_date,settlement_date,account_id";

    private static void writeCsv(Path file, String... lines) throws IOException {
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toleranceStrategySuppressesSmallPriceConflict(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.csv");
        Path b = dir.resolve("b.csv");
        String rowA = "T1,AAPL,BUY,10,182.50,2025-09-15,2025-09-17,ACC-1";
        String rowB = "T1,AAPL,BUY,10,182.55,2025-09-15,2025-09-17,ACC-1";
        writeCsv(a, HEADER, rowA);
        writeCsv(b, HEADER, rowB);

        ReconciliationConfig cfg = new ReconciliationConfig(
                ReconciliationStrategyType.TOLERANCE,
                new BigDecimal("0.10"),
                Broker.A,
                Paths.get("unused"),
                Paths.get("unused"),
                2,
                RoundingMode.HALF_UP);

        TradeCsvParser.LoadResult la = TradeCsvParser.load(Broker.A, a);
        TradeCsvParser.LoadResult lb = TradeCsvParser.load(Broker.B, b);
        ReconciliationResult out = TradeReconciliationService.reconcile(la, lb, cfg);

        assertTrue(out.getConflicts().stream().noneMatch(c -> "price".equals(c.getFieldName())));
        assertEquals(1, out.getUnifiedTrades().size());
        assertEquals(0, new BigDecimal("182.50").compareTo(out.getUnifiedTrades().get(0).getPrice()));
    }

    @Test
    void priorityBrokerStrategyUsesBrokerBRowWhenPricesDiffer(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.csv");
        Path b = dir.resolve("b.csv");
        writeCsv(a, HEADER, "T1,AAPL,BUY,10,182.50,2025-09-15,2025-09-17,ACC-1");
        writeCsv(b, HEADER, "T1,AAPL,BUY,10,182.55,2025-09-15,2025-09-17,ACC-1");

        ReconciliationConfig cfg = new ReconciliationConfig(
                ReconciliationStrategyType.PRIORITY_BROKER,
                BigDecimal.ZERO,
                Broker.B,
                Paths.get("unused"),
                Paths.get("unused"),
                2,
                RoundingMode.HALF_UP);

        TradeCsvParser.LoadResult la = TradeCsvParser.load(Broker.A, a);
        TradeCsvParser.LoadResult lb = TradeCsvParser.load(Broker.B, b);
        ReconciliationResult out = TradeReconciliationService.reconcile(la, lb, cfg);

        assertEquals(1, out.getConflicts().stream().filter(c -> "price".equals(c.getFieldName())).count());
        assertEquals(0, new BigDecimal("182.55").compareTo(out.getUnifiedTrades().get(0).getPrice()));
    }
}
