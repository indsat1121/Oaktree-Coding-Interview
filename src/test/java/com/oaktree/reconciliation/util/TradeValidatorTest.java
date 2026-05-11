package com.oaktree.reconciliation.util;

import com.oaktree.reconciliation.model.TradeData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
@DisplayName("TradeValidator")
class TradeValidatorTest {

    private static TradeData validTrade() {
        TradeData t = new TradeData();
        t.setTradeId("T1");
        t.setSymbol("AAPL");
        t.setSide("BUY");
        t.setQuantity(100);
        t.setPrice(150.25);
        t.setTradeDate(LocalDate.of(2025, 9, 15));
        t.setSettlementDate(LocalDate.of(2025, 9, 17));
        t.setAccountId("ACC-1");
        return t;
    }

    @Nested
    @DisplayName("Positive — fully valid rows")
    class Positive {

        @Test
        void acceptsCompleteValidTrade() {
            assertFalse(TradeValidator.validate(validTrade()).isPresent());
        }

        @Test
        void parseDateAcceptsIsoDateWithSurroundingWhitespace() {
            Optional<LocalDate> d = TradeValidator.parseDate("  2025-01-02  ");
            assertTrue(d.isPresent());
            assertEquals(LocalDate.of(2025, 1, 2), d.get());
        }

        @Test
        void parseDoubleStrictAcceptsIntegerAndDecimalStrings() {
            assertEquals(500.0, TradeValidator.parseDoubleStrict("500").orElse(0.0), 1e-9);
            assertEquals(182.55, TradeValidator.parseDoubleStrict("182.55").orElse(0.0), 1e-9);
        }
    }

    @Nested
    @DisplayName("Negative — validation failures")
    class Negative {

        @Test
        void rejectsBlankTradeId() {
            TradeData t = validTrade();
            t.setTradeId("  ");
            assertEquals("missing trade_id", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void rejectsBlankSymbol() {
            TradeData t = validTrade();
            t.setSymbol("");
            assertEquals("missing symbol", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void rejectsNegativeQuantity() {
            TradeData t = validTrade();
            t.setQuantity(-1);
            assertEquals("negative quantity", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void rejectsZeroQuantity() {
            TradeData t = validTrade();
            t.setQuantity(0);
            assertEquals("non-positive quantity", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void rejectsNonPositivePrice() {
            TradeData t = validTrade();
            t.setPrice(0);
            assertEquals("non-positive price", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void rejectsNaNPrice() {
            TradeData t = validTrade();
            t.setPrice(Double.NaN);
            assertEquals("non-numeric price", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void rejectsNullTradeDate() {
            TradeData t = validTrade();
            t.setTradeDate(null);
            assertEquals("missing trade_date", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void rejectsNullSettlementDate() {
            TradeData t = validTrade();
            t.setSettlementDate(null);
            assertEquals("missing settlement_date", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void rejectsBlankAccountId() {
            TradeData t = validTrade();
            t.setAccountId(null);
            assertEquals("missing account_id", TradeValidator.validate(t).orElse(""));
        }

        @Test
        void parseDateRejectsGarbage() {
            assertFalse(TradeValidator.parseDate("not-a-date").isPresent());
        }

        @Test
        void parseDoubleStrictRejectsNonNumeric() {
            assertFalse(TradeValidator.parseDoubleStrict("abc").isPresent());
        }
    }

    @Nested
    @DisplayName("Edge — boundaries and helpers")
    class Edge {

        @Test
        void acceptsQuantityOneAndMinimalPositivePrice() {
            TradeData t = validTrade();
            t.setQuantity(1);
            t.setPrice(Double.MIN_NORMAL);
            assertFalse(TradeValidator.validate(t).isPresent());
        }

        @Test
        void isBlankTreatsNullAndWhitespace() {
            assertTrue(TradeValidator.isBlank(null));
            assertTrue(TradeValidator.isBlank(""));
            assertTrue(TradeValidator.isBlank("   "));
            assertFalse(TradeValidator.isBlank("x"));
        }

        @Test
        void parseDateEmptyString() {
            assertFalse(TradeValidator.parseDate("").isPresent());
        }

        @Test
        void parseDoubleStrictEmptyString() {
            assertFalse(TradeValidator.parseDoubleStrict("").isPresent());
        }
    }
}
