package com.oaktree.reconciliation.io;

import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.RejectedRecord;
import com.oaktree.reconciliation.model.TradeData;
import com.oaktree.reconciliation.util.TradeValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses broker trade CSV feeds into valid {@link TradeData} rows and {@link RejectedRecord} entries.
 */
public final class TradeCsvParser {

    private static final int EXPECTED_COLUMNS = 8;

    private TradeCsvParser() {
    }

    public static class LoadResult {
        private final List<TradeData> validTrades;
        private final List<RejectedRecord> rejected;
        private final int totalDataRows;
        /** Every {@code trade_id} observed in a data row (including rejected / invalid rows). */
        private final Set<String> tradeIdsAppearingInFile;

        public LoadResult(
                List<TradeData> validTrades,
                List<RejectedRecord> rejected,
                int totalDataRows,
                Set<String> tradeIdsAppearingInFile) {
            this.validTrades = validTrades;
            this.rejected = rejected;
            this.totalDataRows = totalDataRows;
            this.tradeIdsAppearingInFile = tradeIdsAppearingInFile;
        }

        public List<TradeData> getValidTrades() {
            return validTrades;
        }

        public List<RejectedRecord> getRejected() {
            return rejected;
        }

        public int getTotalDataRows() {
            return totalDataRows;
        }

        public Map<String, TradeData> validByTradeId() {
            Map<String, TradeData> map = new LinkedHashMap<>();
            for (TradeData t : validTrades) {
                map.putIfAbsent(t.getTradeId(), t);
            }
            return map;
        }

        public Set<String> getTradeIdsAppearingInFile() {
            return tradeIdsAppearingInFile;
        }
    }

    public static LoadResult load(Broker broker, Path path) throws IOException {
        List<TradeData> valid = new ArrayList<>();
        List<RejectedRecord> rejected = new ArrayList<>();
        int dataRows = 0;
        Set<String> tradeIdsAppearingInFile = new LinkedHashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new LoadResult(valid, rejected, 0, Collections.emptySet());
            }
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                dataRows++;
                String[] cols = trimmed.split(",", -1);
                for (int i = 0; i < cols.length; i++) {
                    cols[i] = cols[i].trim();
                }

                if (cols.length > 0 && !TradeValidator.isBlank(cols[0])) {
                    tradeIdsAppearingInFile.add(cols[0]);
                }

                if (cols.length != EXPECTED_COLUMNS) {
                    String tid = cols.length > 0 ? cols[0] : "";
                    rejected.add(new RejectedRecord(broker, tid, "malformed row (expected " + EXPECTED_COLUMNS + " columns)"));
                    continue;
                }

                Optional<RejectedRecord> reject = parseRow(broker, cols);
                if (reject.isPresent()) {
                    rejected.add(reject.get());
                    continue;
                }

                TradeData trade = buildTrade(cols);
                Optional<String> validationError = TradeValidator.validate(trade);
                if (validationError.isPresent()) {
                    rejected.add(new RejectedRecord(broker, trade.getTradeId(), validationError.get()));
                } else {
                    valid.add(trade);
                }
            }
        }

        return new LoadResult(
                valid,
                rejected,
                dataRows,
                Collections.unmodifiableSet(new LinkedHashSet<>(tradeIdsAppearingInFile)));
    }

    private static Optional<RejectedRecord> parseRow(Broker broker, String[] c) {
        String tradeId = c[0];

        if (TradeValidator.isBlank(c[3])) {
            return Optional.of(new RejectedRecord(broker, tradeId, "missing quantity"));
        }
        Optional<Double> qty = TradeValidator.parseDoubleStrict(c[3]);
        if (!qty.isPresent()) {
            return Optional.of(new RejectedRecord(broker, tradeId, "non-numeric quantity"));
        }

        if (TradeValidator.isBlank(c[4])) {
            return Optional.of(new RejectedRecord(broker, tradeId, "missing price"));
        }
        Optional<Double> price = TradeValidator.parseDoubleStrict(c[4]);
        if (!price.isPresent()) {
            return Optional.of(new RejectedRecord(broker, tradeId, "non-numeric price"));
        }

        if (TradeValidator.isBlank(c[5])) {
            return Optional.of(new RejectedRecord(broker, tradeId, "missing trade_date"));
        }
        Optional<LocalDate> tradeDate = TradeValidator.parseDate(c[5]);
        if (!tradeDate.isPresent()) {
            return Optional.of(new RejectedRecord(broker, tradeId, "invalid trade_date"));
        }

        if (TradeValidator.isBlank(c[6])) {
            return Optional.of(new RejectedRecord(broker, tradeId, "missing settlement_date"));
        }
        Optional<LocalDate> settleDate = TradeValidator.parseDate(c[6]);
        if (!settleDate.isPresent()) {
            return Optional.of(new RejectedRecord(broker, tradeId, "invalid settlement_date"));
        }

        return Optional.empty();
    }

    private static TradeData buildTrade(String[] c) {
        TradeData t = new TradeData();
        t.setTradeId(c[0]);
        t.setSymbol(c[1]);
        t.setSide(c[2]);
        t.setQuantity(Double.parseDouble(c[3].trim()));
        t.setPrice(Double.parseDouble(c[4].trim()));
        t.setTradeDate(TradeValidator.parseDate(c[5]).orElse(null));
        t.setSettlementDate(TradeValidator.parseDate(c[6]).orElse(null));
        t.setAccountId(c[7]);
        return t;
    }
}
