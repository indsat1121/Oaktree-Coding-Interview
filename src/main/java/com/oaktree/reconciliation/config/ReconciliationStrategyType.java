package com.oaktree.reconciliation.config;

import java.util.Locale;

public enum ReconciliationStrategyType {
    EXACT,
    TOLERANCE,
    PRIORITY_BROKER;

    public static ReconciliationStrategyType fromProperty(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return EXACT;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if ("tolerance".equals(s)) {
            return TOLERANCE;
        }
        if ("priority-broker".equals(s) || "prioritybroker".equals(s)) {
            return PRIORITY_BROKER;
        }
        return EXACT;
    }
}
