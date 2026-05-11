package com.oaktree.reconciliation.io;

import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.TradeData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@DisplayName("TradeCsvParser")
class TradeCsvParserTest {

    private static final String HEADER =
            "trade_id,symbol,side,quantity,price,trade_date,settlement_date,account_id";

    private static void writeCsv(Path file, String... lines) throws IOException {
        String body = String.join("\n", lines);
        Files.write(file, body.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("Positive — clean parses")
    class Positive {

        @Test
        void parsesSingleValidRow(@TempDir Path dir) throws IOException {
            Path f = dir.resolve("a.csv");
            writeCsv(f, HEADER, "T1,AAPL,BUY,10,100.00,2025-09-15,2025-09-17,ACC-1");

            TradeCsvParser.LoadResult r = TradeCsvParser.load(Broker.A, f);
            assertEquals(1, r.getTotalDataRows());
            assertEquals(1, r.getValidTrades().size());
            assertEquals(0, r.getRejected().size());

            TradeData t = r.getValidTrades().get(0);
            assertEquals("T1", t.getTradeId());
            assertEquals("AAPL", t.getSymbol());
            assertEquals(10, t.getQuantity(), 1e-9);
            assertEquals(100.0, t.getPrice(), 1e-9);
            assertTrue(r.getTradeIdsAppearingInFile().contains("T1"));
        }

        @Test
        void skipsBlankLinesBetweenRows(@TempDir Path dir) throws IOException {
            Path f = dir.resolve("b.csv");
            writeCsv(
                    f,
                    HEADER,
                    "",
                    "T1,X,BUY,1,1.00,2025-09-15,2025-09-17,ACC-1",
                    "   ",
                    "T2,Y,SELL,2,2.00,2025-09-15,2025-09-17,ACC-2");

            TradeCsvParser.LoadResult r = TradeCsvParser.load(Broker.B, f);
            assertEquals(2, r.getTotalDataRows());
            assertEquals(2, r.getValidTrades().size());
        }
    }

    @Nested
    @DisplayName("Negative — rejects bad rows")
    class Negative {

        @Test
        void rejectsMissingPrice(@TempDir Path dir) throws IOException {
            Path f = dir.resolve("bad.csv");
            writeCsv(f, HEADER, "T9,AMZN,BUY,250,,2025-09-15,2025-09-17,ACC-3");

            TradeCsvParser.LoadResult r = TradeCsvParser.load(Broker.A, f);
            assertEquals(1, r.getTotalDataRows());
            assertEquals(0, r.getValidTrades().size());
            assertEquals(1, r.getRejected().size());
            assertEquals("missing price", r.getRejected().get(0).getReason());
            assertEquals("T9", r.getRejected().get(0).getTradeId());
        }

        @Test
        void rejectsNegativeQuantity(@TempDir Path dir) throws IOException {
            Path f = dir.resolve("neg.csv");
            writeCsv(f, HEADER, "T4,TSLA,BUY,-300,1.00,2025-09-15,2025-09-17,ACC-1");

            TradeCsvParser.LoadResult r = TradeCsvParser.load(Broker.B, f);
            assertEquals(1, r.getRejected().size());
            assertEquals("negative quantity", r.getRejected().get(0).getReason());
        }

        @Test
        void rejectsMalformedColumnCount(@TempDir Path dir) throws IOException {
            Path f = dir.resolve("mal.csv");
            writeCsv(f, HEADER, "only,three,cols");

            TradeCsvParser.LoadResult r = TradeCsvParser.load(Broker.A, f);
            assertEquals(1, r.getRejected().size());
            assertTrue(r.getRejected().get(0).getReason().contains("malformed"));
        }

        @Test
        void headerOnlyYieldsNoDataRows(@TempDir Path dir) throws IOException {
            Path f = dir.resolve("empty.csv");
            writeCsv(f, HEADER);

            TradeCsvParser.LoadResult r = TradeCsvParser.load(Broker.A, f);
            assertEquals(0, r.getTotalDataRows());
            assertEquals(0, r.getValidTrades().size());
        }
    }

    @Nested
    @DisplayName("Edge — duplicate ids and empty file")
    class Edge {

        @Test
        void duplicateTradeIdKeepsFirstValidOccurrence(@TempDir Path dir) throws IOException {
            Path f = dir.resolve("dup.csv");
            writeCsv(
                    f,
                    HEADER,
                    "T1,AAPL,BUY,1,10.00,2025-09-15,2025-09-17,ACC-1",
                    "T1,MSFT,SELL,99,99.00,2025-09-15,2025-09-17,ACC-2");

            TradeCsvParser.LoadResult r = TradeCsvParser.load(Broker.A, f);
            assertEquals(2, r.getTotalDataRows());
            assertEquals(2, r.getValidTrades().size());
            Optional<TradeData> one = r.getValidTrades().stream().filter(t -> "T1".equals(t.getTradeId())).findFirst();
            assertTrue(one.isPresent());
            assertEquals("AAPL", one.get().getSymbol());
            assertEquals(1, r.validByTradeId().size());
            assertEquals("AAPL", r.validByTradeId().get("T1").getSymbol());
        }

        @Test
        void nullFileContentOnlyHeaderHandled(@TempDir Path dir) throws IOException {
            Path f = dir.resolve("onlyheader.csv");
            Files.write(f, (HEADER + "\n").getBytes(StandardCharsets.UTF_8));

            TradeCsvParser.LoadResult r = TradeCsvParser.load(Broker.A, f);
            assertEquals(0, r.getTotalDataRows());
        }
    }
}
