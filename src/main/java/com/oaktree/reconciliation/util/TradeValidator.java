package com.oaktree.reconciliation.util;

import com.oaktree.reconciliation.model.TradeData;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        if (t.getQuantity() < 0) {
            return Optional.of("negative quantity");
        }
        if (t.getQuantity() <= 0) {
            return Optional.of("non-positive quantity");
        }
        if (Double.isNaN(t.getPrice()) || Double.isInfinite(t.getPrice())) {
            return Optional.of("non-numeric price");
        }
        if (t.getPrice() <= 0) {
            return Optional.of("non-positive price");
        }
        if (t.getTradeDate() == null) {
            return Optional.of("missing trade_date");
        }
        if (t.getSettlementDate() == null) {
            return Optional.of("missing settlement_date");
        }
        if (isBlank(t.getAccountId())) {
            return Optional.of("missing account_id");
        }
        return Optional.empty();
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
}
