package com.oaktree.reconciliation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Plain-text formatting for monetary and quantity fields (reconciliation output).
 */
public final class TradeAmountFormatter {

    private TradeAmountFormatter() {
    }

    public static String formatPrice(BigDecimal price) {
        if (price == null) {
            return "";
        }
        return price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Human-friendly quantity string (no unnecessary trailing fraction zeros).
     * Java 8–compatible: avoids {@code BigDecimal.stripTrailingZeros()} (added in Java 9).
     */
    public static String formatQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return "";
        }
        return trimFractionalTrailingZeros(quantity.toPlainString());
    }

    private static String trimFractionalTrailingZeros(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        int dot = plain.indexOf('.');
        if (dot < 0) {
            return plain;
        }
        int end = plain.length();
        while (end > dot + 1 && plain.charAt(end - 1) == '0') {
            end--;
        }
        if (end > dot && plain.charAt(end - 1) == '.') {
            end--;
        }
        return plain.substring(0, end);
    }
}
