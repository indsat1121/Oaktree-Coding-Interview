package com.oaktree.reconciliation.service;

import com.oaktree.reconciliation.io.TradeCsvParser;
import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.ReconciliationResult;
import com.oaktree.reconciliation.model.ReconciliationSummary;
import com.oaktree.reconciliation.model.TradeData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@DisplayName("TradeReconciliationService")
class TradeReconciliationServiceTest {

    private static final String HEADER =
            "trade_id,symbol,side,quantity,price,trade_date,settlement_date,account_id";

    private static void writeCsv(Path file, String... lines) throws IOException {
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("Positive — happy paths")
    class Positive {

        @Test
        void identicalFeedsProduceNoConflictsAndSingleUnified(@TempDir Path dir) throws IOException {
            Path a = dir.resolve("a.csv");
            Path b = dir.resolve("b.csv");
            String row = "T1,AAPL,BUY,10,100.00,2025-09-15,2025-09-17,ACC-1";
            writeCsv(a, HEADER, row);
            writeCsv(b, HEADER, row);

            TradeCsvParser.LoadResult la = TradeCsvParser.load(Broker.A, a);
            TradeCsvParser.LoadResult lb = TradeCsvParser.load(Broker.B, b);
            ReconciliationResult out = TradeReconciliationService.reconcile(la, lb);

            assertEquals(1, out.getUnifiedTrades().size());
            assertEquals(0, out.getConflicts().size());
            assertEquals(1, out.getSummary().getMatchedCount());
            assertEquals("T1", out.getUnifiedTrades().get(0).getTradeId());
        }

        @Test
        void brokerAWinsOnPriceConflict(@TempDir Path dir) throws IOException {
            Path a = dir.resolve("a.csv");
            Path b = dir.resolve("b.csv");
            writeCsv(a, HEADER, "T1,AAPL,BUY,10,182.50,2025-09-15,2025-09-17,ACC-1");
            writeCsv(b, HEADER, "T1,AAPL,BUY,10,182.55,2025-09-15,2025-09-17,ACC-1");

            ReconciliationResult out = TradeReconciliationService.reconcile(
                    TradeCsvParser.load(Broker.A, a),
                    TradeCsvParser.load(Broker.B, b));

            assertEquals(182.50, out.getUnifiedTrades().get(0).getPrice(), 1e-6);
            assertEquals(1, out.getConflicts().size());
            assertEquals("price", out.getConflicts().get(0).getFieldName());
        }
    }

    @Nested
    @DisplayName("Negative — rejections still aggregate")
    class Negative {

        @Test
        void rejectedRowsAppearInResult(@TempDir Path dir) throws IOException {
            Path a = dir.resolve("a.csv");
            Path b = dir.resolve("b.csv");
            writeCsv(a, HEADER, "T1,X,BUY,1,1.00,2025-09-15,2025-09-17,ACC-1");
            writeCsv(b, HEADER, "T2,Y,SELL,1,,2025-09-15,2025-09-17,ACC-2");

            ReconciliationResult out = TradeReconciliationService.reconcile(
                    TradeCsvParser.load(Broker.A, a),
                    TradeCsvParser.load(Broker.B, b));

            assertEquals(1, out.getRejectedRecords().size());
            assertEquals("missing price", out.getRejectedRecords().get(0).getReason());
        }
    }

    @Nested
    @DisplayName("Edge — empty and asymmetric feeds")
    class Edge {

        @Test
        void bothFeedsEmptyExceptHeaders(@TempDir Path dir) throws IOException {
            Path a = dir.resolve("a.csv");
            Path b = dir.resolve("b.csv");
            writeCsv(a, HEADER);
            writeCsv(b, HEADER);

            ReconciliationResult out = TradeReconciliationService.reconcile(
                    TradeCsvParser.load(Broker.A, a),
                    TradeCsvParser.load(Broker.B, b));

            assertEquals(0, out.getUnifiedTrades().size());
            ReconciliationSummary s = out.getSummary();
            assertEquals(0, s.getMatchedCount());
            assertEquals(0, s.getConflictCount());
        }

        @Test
        void validOnlyInAWhenIdNotInBFile(@TempDir Path dir) throws IOException {
            Path a = dir.resolve("a.csv");
            Path b = dir.resolve("b.csv");
            writeCsv(
                    a,
                    HEADER,
                    "T1,A,BUY,1,1.00,2025-09-15,2025-09-17,ACC-1",
                    "T2,B,SELL,2,2.00,2025-09-15,2025-09-17,ACC-2");
            writeCsv(b, HEADER, "T9,Z,BUY,1,1.00,2025-09-15,2025-09-17,ACC-9");

            ReconciliationResult out = TradeReconciliationService.reconcile(
                    TradeCsvParser.load(Broker.A, a),
                    TradeCsvParser.load(Broker.B, b));

            Set<String> ids = out.getUnifiedTrades().stream().map(TradeData::getTradeId).collect(Collectors.toSet());
            assertTrue(ids.contains("T1"));
            assertTrue(ids.contains("T2"));
            assertTrue(ids.contains("T9"));
            assertEquals(3, out.getUnifiedTrades().size());
            assertEquals(2, out.getSummary().getUnmatchedAOnly());
            assertEquals(1, out.getSummary().getUnmatchedBOnly());
        }

        @Test
        void sameIdInBothFilesButOnlyOneValidExcludesFromUnified(@TempDir Path dir) throws IOException {
            Path a = dir.resolve("a.csv");
            Path b = dir.resolve("b.csv");
            writeCsv(a, HEADER, "T4,TSLA,BUY,300,1.00,2025-09-15,2025-09-17,ACC-1");
            writeCsv(b, HEADER, "T4,TSLA,BUY,-300,1.00,2025-09-15,2025-09-17,ACC-1");

            ReconciliationResult out = TradeReconciliationService.reconcile(
                    TradeCsvParser.load(Broker.A, a),
                    TradeCsvParser.load(Broker.B, b));

            List<String> unifiedIds = out.getUnifiedTrades().stream().map(TradeData::getTradeId).collect(Collectors.toList());
            assertTrue(unifiedIds.stream().noneMatch("T4"::equals));
            assertEquals(0, out.getSummary().getMatchedCount());
        }
    }
}
