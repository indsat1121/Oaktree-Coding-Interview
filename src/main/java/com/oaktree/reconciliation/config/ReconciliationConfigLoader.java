package com.oaktree.reconciliation.config;

import com.oaktree.reconciliation.model.Broker;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads {@link ReconciliationConfig} from classpath {@code application.properties}, then applies environment overrides.
 */
public final class ReconciliationConfigLoader {

    private static final String PROP_STRATEGY = "reconciliation.strategy";
    private static final String PROP_TOLERANCE = "reconciliation.price-tolerance";
    private static final String PROP_PRIMARY = "reconciliation.primary-broker";
    private static final String PROP_FEED_A = "trade.feed.path.a";
    private static final String PROP_FEED_B = "trade.feed.path.b";
    private static final String PROP_PRICE_SCALE = "financial.price.scale";
    private static final String PROP_ROUNDING = "financial.price.rounding-mode";

    private static final String ENV_STRATEGY = "RECONCILIATION_STRATEGY";
    private static final String ENV_TOLERANCE = "RECONCILIATION_PRICE_TOLERANCE";
    private static final String ENV_PRIMARY = "RECONCILIATION_PRIMARY_BROKER";
    private static final String ENV_FEED_A = "TRADE_FEED_A";
    private static final String ENV_FEED_B = "TRADE_FEED_B";
    private static final String ENV_PRICE_SCALE = "FINANCIAL_PRICE_SCALE";
    private static final String ENV_ROUNDING = "FINANCIAL_PRICE_ROUNDING_MODE";

    private ReconciliationConfigLoader() {
    }

    public static ReconciliationConfig load() throws IOException {
        Properties props = new Properties();
        try (InputStream in = ReconciliationConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        }

        ReconciliationStrategyType type = ReconciliationStrategyType.fromProperty(
                firstNonBlank(env(ENV_STRATEGY), props.getProperty(PROP_STRATEGY)));

        BigDecimal tolerance = parseBigDecimal(
                firstNonBlank(env(ENV_TOLERANCE), props.getProperty(PROP_TOLERANCE)),
                BigDecimal.ZERO);

        Broker primary = ReconciliationConfig.parsePrimaryBroker(
                firstNonBlank(env(ENV_PRIMARY), props.getProperty(PROP_PRIMARY)));

        Path pathA = Paths.get(Objects.requireNonNull(
                firstNonBlank(env(ENV_FEED_A), props.getProperty(PROP_FEED_A), "files/Trade_A.csv")));
        Path pathB = Paths.get(Objects.requireNonNull(
                firstNonBlank(env(ENV_FEED_B), props.getProperty(PROP_FEED_B), "files/Trade_B.csv")));

        int scale = parsePositiveInt(firstNonBlank(env(ENV_PRICE_SCALE), props.getProperty(PROP_PRICE_SCALE)), 2);
        RoundingMode rounding = ReconciliationConfig.parseRoundingMode(
                firstNonBlank(env(ENV_ROUNDING), props.getProperty(PROP_ROUNDING)));

        return new ReconciliationConfig(type, tolerance, primary, pathA, pathB, scale, rounding);
    }

    private static String env(String name) {
        String v = System.getenv(name);
        return v == null ? "" : v.trim();
    }

    @SafeVarargs
    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    private static BigDecimal parseBigDecimal(String raw, BigDecimal defaultVal) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultVal;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static int parsePositiveInt(String raw, int defaultVal) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultVal;
        }
        try {
            int n = Integer.parseInt(raw.trim());
            return n > 0 ? n : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
