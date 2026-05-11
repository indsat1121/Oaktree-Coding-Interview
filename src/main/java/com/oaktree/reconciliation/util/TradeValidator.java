package com.oaktree.reconciliation.util;

import com.oaktree.reconciliation.model.TradeData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

public final class TradeValidator {

    private TradeValidator() {
    }

    /**
     * @return empty if valid, otherwise a single human-readable rejection reason
     */
    public static Optional<String> validate(TradeData t) {
        if (isBlank(t.getTradeId())) {
            return Optional.of("missing trade_id");
        }
        if (isBlank(t.getSymbol())) {
            return Optional.of("missing symbol");
        }
        if (isBlank(t.getSide())) {
            return Optional.of("missing side");
        }
        if (!isAllowedSide(t.getSide())) {
            return Optional.of("invalid side (must be BUY or SELL)");
        }
        if (t.getQuantity() == null) {
            return Optional.of("missing quantity");
        }
        if (t.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            return Optional.of("negative quantity");
        }
        if (t.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of("non-positive quantity");
        }
        if (t.getPrice() == null) {
            return Optional.of("missing price");
        }
        if (t.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of("non-positive price");
        }
        if (t.getTradeDate() == null) {
            return Optional.of("missing trade_date");
        }
        if (t.getSettlementDate() == null) {
            return Optional.of("missing settlement_date");
        }
        if (t.getSettlementDate().isBefore(t.getTradeDate())) {
            return Optional.of("settlement_date before trade_date");
        }
        if (isBlank(t.getAccountId())) {
            return Optional.of("missing account_id");
        }
        return Optional.empty();
    }

    public static boolean isAllowedSide(String side) {
        if (isBlank(side)) {
            return false;
        }
        String u = side.trim().toUpperCase(Locale.ROOT);
        return "BUY".equals(u) || "SELL".equals(u);
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static Optional<LocalDate> parseDate(String raw) {
        if (isBlank(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(raw.trim()));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static Optional<Double> parseDoubleStrict(String raw) {
        if (isBlank(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<BigDecimal> parseBigDecimalStrict(String raw) {
        if (isBlank(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(raw.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
