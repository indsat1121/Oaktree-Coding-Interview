# Trade feed reconciliation

Small Java module that ingests two broker CSV feeds for the same day, validates rows, matches trades by `trade_id`, reports field-level conflicts, and emits a unified trade list plus a summary.

## Requirements

- **JDK** 8 or newer (must include `javac`, not a JRE-only install such as the legacy Java browser plug-in)
- **Cursor / VS Code:** Open this **repository root** (the folder that contains `pom.xml`), not a parent directory. Install the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) when Cursor prompts for **recommended workspace extensions** (see `.vscode/extensions.json`). Wait until the status bar finishes **“Opening Java projects / Importing Maven project(s)…”**. If Java still does not activate: Command Palette → **Java: Clean Java Language Server Workspace** → reload.

If every `.java` file stays red, pick a **JDK** for the tooling: Command Palette → **Java: Configure Java Runtime** → **JDK** for the workspace or for **Java LS** (language server). Plain JRE installs are not enough for navigation and errors.

This repo ships **`.vscode/settings.json`**, **`launch.json`**, **`tasks.json`**, and **`extensions.json`** so the workspace is treated as a **Maven Java** project.

### Maven: “No compiler is provided… JRE rather than a JDK”

`mvn` uses `JAVA_HOME`. If it points at a **JRE**, compilation fails. Fix either:

1. **macOS:** install a **JDK** (not only a JRE), then e.g. `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` (adjust the version flag to match what you installed).  
2. Run **`./scripts/mvn test`** (or **`make test`**): the script tries `/usr/libexec/java_home` versions `21` → `1.8` and picks the first home that contains `bin/javac`.

#### If `brew install …` crashes (e.g. `version.rb … Version value must be a string`)

That error comes from a **broken or very old Homebrew** under `/usr/local`, not from Java. You do **not** need Homebrew to get a JDK:

- **Installer (simplest):** [Eclipse Temurin 17 (LTS) macOS `.pkg`)](https://adoptium.net/temurin/releases/?os=mac&arch=any&package=jdk&version=17) — run the installer, then `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` (or pick the shown JVM from `/usr/libexec/java_home -V`).
- **Apple’s builds:** [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or Amazon Corretto — same idea: install, then point `JAVA_HOME` at the new `Contents/Home` path.

To **repair Homebrew** (optional, for other tools): install the current script from [brew.sh](https://brew.sh/) (Apple Silicon uses `/opt/homebrew`; old Intel installs in `/usr/local` often need a fresh install or `brew update-reset` once `brew` runs again).

Check what you already have:

```bash
/usr/libexec/java_home -V
```

## Project layout

```text
files/                          Sample broker CSV feeds
scripts/mvn                     Wrapper: pick a JDK with javac, then run Maven
Makefile                        make compile | make test | make run
src/main/java/…                 Java sources
src/test/java/…                 JUnit tests
target/classes/                 Maven compile output (after ./scripts/mvn compile)
bin/                            Optional: manual javac output (gitignored)
```

## Quick start

Run everything from the **repository root** (the folder that contains `pom.xml`) so default paths `files/Trade_A.csv` and `files/Trade_B.csv` resolve.

### Compile

```bash
./scripts/mvn compile
```

Or: `make compile`

If `JAVA_HOME` already points at a JDK that includes `javac`, you can use `mvn compile` instead of `./scripts/mvn`.

### Run the program

Compile (if needed), then run with `target/classes` on the classpath:

```bash
./scripts/mvn -q compile && java -cp target/classes com.oaktree.reconciliation.TradeReconciliationMain
```

If you already compiled:

```bash
java -cp target/classes com.oaktree.reconciliation.TradeReconciliationMain
```

Or: `make run` (compiles via Maven, then runs the same `java` line).

**Custom CSV paths** (Broker A file, then Broker B file):

```bash
java -cp target/classes com.oaktree.reconciliation.TradeReconciliationMain /path/to/Trade_A.csv /path/to/Trade_B.csv
```

### Unit tests

Tests live under `src/test/java` (JUnit 5).

```bash
./scripts/mvn test
```

Or: `make test`

### Optional: compile without Maven

If you do not use Maven, you can compile all main sources into `bin/` and run from there (tests are not included in this snippet):

```bash
mkdir -p bin
javac -encoding UTF-8 -d bin $(find src/main/java -name '*.java')
java -cp bin com.oaktree.reconciliation.TradeReconciliationMain
```

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

## Packages

| Package | Types |
|---------|--------|
| `com.oaktree.reconciliation` | `TradeReconciliationMain` |
| `com.oaktree.reconciliation.model` | `TradeData`, `Broker`, `RejectedRecord`, `FieldConflict`, `ReconciliationResult`, `ReconciliationSummary` |
| `com.oaktree.reconciliation.io` | `TradeCsvParser` |
| `com.oaktree.reconciliation.service` | `TradeReconciliationService` |
| `com.oaktree.reconciliation.util` | `TradeValidator` |
