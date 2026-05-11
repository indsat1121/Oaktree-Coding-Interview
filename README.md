# Trade feed reconciliation

Small Java module that ingests two broker CSV feeds for the same day, validates rows, matches trades by `trade_id`, reports field-level conflicts, and emits a unified trade list plus a summary.

## Requirements

- JDK 8 or newer (`javac` and `java` on your `PATH`)

## Quick start

From the repository root:

```bash
javac src/*.java
java -cp src TradeReconciliationMain
```

By default this reads `src/Trade_A.csv` (Broker A) and `src/Trade_B.csv` (Broker B).

### Custom file paths

```bash
java -cp src TradeReconciliationMain /path/to/Trade_A.csv /path/to/Trade_B.csv
```

The first argument is Broker A’s feed; the second is Broker B’s feed.

## Input format

UTF-8 CSV with a header row, then one trade per line:

```text
trade_id,symbol,side,quantity,price,trade_date,settlement_date,account_id
```

- Dates use ISO-8601 calendar dates (for example `2025-09-15`).
- `quantity` and `price` are decimal numbers. The parser uses a simple comma split (no embedded commas in fields).

## What the program does

1. **Parse and validate** each row. Invalid rows are rejected with a reason (for example missing price, negative quantity, missing settlement date, non-numeric values). Rejections are printed under **Rejected records**.

2. **Match** trades that have the same `trade_id` in **both** feeds **after** validation. Only rows that are valid in A and valid in B count as matched.

3. **Conflicts** — For each matched pair, compare symbol, side, quantity, price, trade and settlement dates, and account id. Disagreements are listed under **Conflict report** (Broker A vs Broker B values).

4. **Unified trades** — Includes:
   - Every matched trade, using **Broker A** as the source of truth when values differ.
   - Valid trades from A whose `trade_id` does **not** appear anywhere in B’s file.
   - Valid trades from B whose `trade_id` does **not** appear anywhere in A’s file.

   If the same `trade_id` appears in both files but only one side validates (for example A valid, B rejected), that id is **not** included in the unified list.

5. **Summary** — Per-broker totals, rejected and valid counts, number of matched trades, number of field conflicts, and counts of unmatched valid trades (A-only / B-only by presence of the id in the other broker’s file).

## Main source layout

| File | Purpose |
|------|---------|
| `TradeReconciliationMain.java` | Entry point and console report |
| `TradeCsvParser.java` | CSV load, row parse, `LoadResult` (valid, rejected, ids seen in file) |
| `TradeValidator.java` | Business validation on `Trade_Data` |
| `TradeReconciliationService.java` | Match, conflict detection, unified list, summary |
| `Trade_Data.java` | Trade row model |
| `Broker.java` | Broker A / B labels |
| `RejectedRecord.java`, `FieldConflict.java` | Rejection and conflict DTOs |
| `ReconciliationResult.java`, `ReconciliationSummary.java` | Aggregated output |

Sample inputs: `src/Trade_A.csv`, `src/Trade_B.csv`.
