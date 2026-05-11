package com.oaktree.reconciliation.config;

import com.oaktree.reconciliation.model.Broker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

/**
 * Externalized reconciliation settings (typically from {@code application.properties} and environment).
 */
public final class ReconciliationConfig {

    private final ReconciliationStrategyType strategyType;
    private final BigDecimal priceTolerance;
    private final Broker primaryBroker;
    private final Path feedPathA;
    private final Path feedPathB;
    private final int priceDisplayScale;
    private final RoundingMode priceRoundingMode;

    public ReconciliationConfig(
            ReconciliationStrategyType strategyType,
            BigDecimal priceTolerance,
            Broker primaryBroker,
            Path feedPathA,
            Path feedPathB,
            int priceDisplayScale,
            RoundingMode priceRoundingMode) {
        this.strategyType = Objects.requireNonNull(strategyType, "strategyType");
        this.priceTolerance = priceTolerance != null ? priceTolerance : BigDecimal.ZERO;
        this.primaryBroker = primaryBroker != null ? primaryBroker : Broker.A;
        this.feedPathA = Objects.requireNonNull(feedPathA, "feedPathA");
        this.feedPathB = Objects.requireNonNull(feedPathB, "feedPathB");
        this.priceDisplayScale = priceDisplayScale;
        this.priceRoundingMode = priceRoundingMode != null ? priceRoundingMode : RoundingMode.HALF_UP;
    }

    public static ReconciliationConfig defaultConfig() {
        return new ReconciliationConfig(
                ReconciliationStrategyType.EXACT,
                BigDecimal.ZERO,
                Broker.A,
                Paths.get("files", "Trade_A.csv"),
                Paths.get("files", "Trade_B.csv"),
                2,
                RoundingMode.HALF_UP);
    }

    public ReconciliationStrategyType getStrategyType() {
        return strategyType;
    }

    public BigDecimal getPriceTolerance() {
        return priceTolerance;
    }

    public Broker getPrimaryBroker() {
        return primaryBroker;
    }

    public Path getFeedPathA() {
        return feedPathA;
    }

    public Path getFeedPathB() {
        return feedPathB;
    }

    public int getPriceDisplayScale() {
        return priceDisplayScale;
    }

    public RoundingMode getPriceRoundingMode() {
        return priceRoundingMode;
    }

    /** Returns a copy with different feed paths (e.g. CLI overrides). */
    public ReconciliationConfig withFeeds(Path pathA, Path pathB) {
        return new ReconciliationConfig(
                strategyType,
                priceTolerance,
                primaryBroker,
                Objects.requireNonNull(pathA, "pathA"),
                Objects.requireNonNull(pathB, "pathB"),
                priceDisplayScale,
                priceRoundingMode);
    }

    public static Broker parsePrimaryBroker(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Broker.A;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("B".equals(u) || "BROKER_B".equals(u) || Broker.B.name().equals(u)) {
            return Broker.B;
        }
        return Broker.A;
    }

    public static RoundingMode parseRoundingMode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return RoundingMode.HALF_UP;
        }
        try {
            return RoundingMode.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return RoundingMode.HALF_UP;
        }
    }
}
