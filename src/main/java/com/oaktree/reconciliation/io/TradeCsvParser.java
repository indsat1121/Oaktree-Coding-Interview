package com.oaktree.reconciliation.io;

import com.oaktree.reconciliation.model.Broker;
import com.oaktree.reconciliation.model.RejectedRecord;
import com.oaktree.reconciliation.model.TradeData;
import com.oaktree.reconciliation.util.TradeValidator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses broker trade CSV feeds (RFC-style via Apache Commons CSV) into valid {@link TradeData} rows
 * and {@link RejectedRecord} entries.
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
        Set<String> seenValidTradeIds = new HashSet<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                CSVParser csvParser = format.parse(reader)) {
            for (CSVRecord record : csvParser) {
                dataRows++;
                if (record.size() > 0 && !TradeValidator.isBlank(record.get(0))) {
                    tradeIdsAppearingInFile.add(record.get(0).trim());
                }

                if (record.size() != EXPECTED_COLUMNS) {
                    String tid = record.size() > 0 ? record.get(0).trim() : "";
                    rejected.add(new RejectedRecord(broker, tid, "malformed row (expected " + EXPECTED_COLUMNS + " columns)"));
                    continue;
                }

                String[] cols = recordToColumns(record);

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
                    if (!seenValidTradeIds.add(trade.getTradeId())) {
                        rejected.add(new RejectedRecord(broker, trade.getTradeId(), "duplicate trade_id in feed"));
                    } else {
                        valid.add(trade);
                    }
                }
            }
        }

        return new LoadResult(
                valid,
                rejected,
                dataRows,
                Collections.unmodifiableSet(new LinkedHashSet<>(tradeIdsAppearingInFile)));
    }

    private static String[] recordToColumns(CSVRecord record) {
        int n = record.size();
        String[] cols = new String[EXPECTED_COLUMNS];
        for (int i = 0; i < EXPECTED_COLUMNS; i++) {
            cols[i] = i < n && record.get(i) != null ? record.get(i).trim() : "";
        }
        return cols;
    }

    private static Optional<RejectedRecord> parseRow(Broker broker, String[] c) {
        String tradeId = c[0];

        if (TradeValidator.isBlank(c[3])) {
            return Optional.of(new RejectedRecord(broker, tradeId, "missing quantity"));
        }
        if (!TradeValidator.parseBigDecimalStrict(c[3]).isPresent()) {
            return Optional.of(new RejectedRecord(broker, tradeId, "non-numeric quantity"));
        }

        if (TradeValidator.isBlank(c[4])) {
            return Optional.of(new RejectedRecord(broker, tradeId, "missing price"));
        }
        if (!TradeValidator.parseBigDecimalStrict(c[4]).isPresent()) {
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
        t.setSide(c[2].trim().toUpperCase(java.util.Locale.ROOT));
        t.setQuantity(TradeValidator.parseBigDecimalStrict(c[3]).orElse(null));
        t.setPrice(TradeValidator.parseBigDecimalStrict(c[4]).orElse(null));
        t.setTradeDate(TradeValidator.parseDate(c[5]).orElse(null));
        t.setSettlementDate(TradeValidator.parseDate(c[6]).orElse(null));
        t.setAccountId(c[7]);
        return t;
    }
}
