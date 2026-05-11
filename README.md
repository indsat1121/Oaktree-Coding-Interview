# Trade Feed Reconciliation

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
src/main/resources/             `application.properties` (reconciliation + feed paths)
src/main/java/…                 Java sources (`config`, `strategy`, `model`, `io`, `service`, `util`, `report`)
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

The app depends on **Apache Commons CSV** at runtime. Easiest is Maven **exec** (puts `target/classes` plus dependencies on the classpath):

```bash
./scripts/mvn -q compile exec:java
```

Or: `make run`

**Custom CSV paths** (Broker A file, then Broker B file):

```bash
./scripts/mvn -q compile exec:java -Dexec.args="files/Trade_A.csv files/Trade_B.csv"
```

(Replace with your two paths; `exec.args` is a single string, so quote paths that contain spaces.)

### Unit tests

Tests live under `src/test/java` (JUnit 5).

```bash
./scripts/mvn test
```

Or: `make test`

### Optional: compile without Maven

Not recommended: you would need **commons-csv** on the classpath as well as all `src/main/java` sources. Prefer `./scripts/mvn compile exec:java` above.

## Input format

UTF-8 CSV with a header row, then one trade per line:

```text
trade_id,symbol,side,quantity,price,trade_date,settlement_date,account_id
```

- Dates use ISO-8601 calendar dates (for example `2025-09-15`).
- `quantity` and `price` are decimal numbers. Rows are read with **Apache Commons CSV** (RFC-style parsing, including quoted fields that contain commas).

## What the program does

1. **Parse and validate** each row. Invalid rows are rejected with a reason (for example missing price, negative quantity, missing settlement date, non-numeric values). Rejections are printed under **Rejected records**.

2. **Match** trades that have the same `trade_id` in **both** feeds **after** validation. Only rows that are valid in A and valid in B count as matched.

3. **Conflicts** — For each matched pair, compare symbol, side, quantity, price, trade and settlement dates, and account id. Disagreements are listed under **Conflict report** (Broker A vs Broker B values).

4. **Unified trades** — Includes:
   - Every matched trade. **Which broker wins on disagreements** depends on the active **reconciliation strategy** and **`reconciliation.primary-broker`** (see below). The default **`exact`** strategy keeps the original exercise behaviour: **Broker A** is the source of truth for the unified row when fields differ.
   - Valid trades from A whose `trade_id` does **not** appear anywhere in B’s file.
   - Valid trades from B whose `trade_id` does **not** appear anywhere in A’s file.

   If the same `trade_id` appears in both files but only one side validates (for example A valid, B rejected), that id is **not** included in the unified list.

   **Quantities and prices** are carried as **`BigDecimal`** end-to-end; report and conflict strings use the configured **scale** and **rounding mode** from `application.properties` (or env overrides).

5. **Summary** — Per-broker totals, rejected and valid counts, number of matched trades, number of field conflicts, and counts of unmatched valid trades (A-only / B-only by presence of the id in the other broker’s file).

## Improvements vs. original GitHub `main`

This section compares **this repository** to the baseline described in the public README and file tree on [**`indsat1121/Oaktree-Coding-Interview` → `main`**](https://github.com/indsat1121/Oaktree-Coding-Interview/tree/main) (flat `src/`, `javac src/*.java`, `Trade_Data`, comma-split CSV, no Maven in docs).

| Area | Original ([`main` on GitHub](https://github.com/indsat1121/Oaktree-Coding-Interview/tree/main)) | This codebase |
|------|------------------------------------------------------------------------------------------------|---------------|
| **Build** | Manual `javac src/*.java`; no `pom.xml` in README | **Maven** — `pom.xml`, dependencies (Commons CSV, JUnit), `maven-compiler-plugin`, `exec-maven-plugin`, `./scripts/mvn` helper for JDK selection |
| **Run** | `java -cp src TradeReconciliationMain` | **`mvn compile exec:java`** / `make run` — runtime classpath includes libraries (e.g. Commons CSV) |
| **Source layout** | Single `src/` folder, default package | **`src/main/java`** / **`src/test/java`**, packages under `com.oaktree.reconciliation` |
| **Sample CSV paths** | README: `src/Trade_A.csv`, `src/Trade_B.csv` | **`files/Trade_A.csv`**, **`files/Trade_B.csv`** (defaults in `TradeReconciliationMain`) |
| **Trade row model** | `Trade_Data`, snake_case getters / `double` amounts | **`TradeData`**, camelCase (`tradeId`, …), **`BigDecimal`** for quantity and price |
| **CSV parsing** | README: simple comma split | **Apache Commons CSV** — headers, trim, RFC-style rows, **quoted fields with commas** |
| **Validation** | Core rules from the exercise | Same core rules **plus**: **BUY/SELL** only (case-insensitive), **settlement_date ≥ trade_date**, **duplicate `trade_id`** in one feed rejected after first valid row |
| **Reconciliation vs. UI** | Report printing tied to `main` | **`TradeReconciliationService`** for logic; **`ReconciliationReportPrinter`** + **`TradeAmountFormatter`** for text output |
| **Types / collections** | Implicit / minimal typing in small codebase | **Explicit generics** on `List`, `Map`, `Set`, `Optional` where used |
| **Tests** | Not described on GitHub README | **JUnit 5** — `TradeCsvParserTest`, `TradeValidatorTest`, `TradeReconciliationServiceTest`, `ReconciliationStrategyTest` |
| **CI** | No workflow in that README snapshot | **`.github/workflows/maven.yml`** — `mvn test` on push/PR to `main` / `master` |
| **IDE / editor** | Not covered | **`.vscode/`** — recommended Java extensions, settings, **launch** / **tasks** for build & run |
| **Docs** | Short README (compile/run only) | Extended README — Maven/JDK issues (e.g. JRE-only `JAVA_HOME`), Homebrew caveats, **Makefile**, improvement table |
| **Reconciliation rules** | Fixed “A wins” behaviour implied by sample | **Strategy pattern** — pluggable `ReconciliationStrategy` (`exact`, `tolerance`, `priority-broker`) |
| **Configuration** | Hard-coded paths / behaviour | **`application.properties`** + **environment variable** overrides for strategy, tolerance, primary broker, feed paths, price scale/rounding |

### Strategy pattern, external configuration, and `BigDecimal` (not on upstream `main`)

The public baseline on [**`indsat1121/Oaktree-Coding-Interview` → `main`**](https://github.com/indsat1121/Oaktree-Coding-Interview/tree/main) does **not** include the items below; they reflect how this codebase would evolve toward **production-style** reconciliation.

| Topic | What was added |
|--------|----------------|
| **Strategy pattern** | `ReconciliationStrategy` with implementations **`ExactMatchStrategy`** (strict field compare; unified matched row always Broker **A** when values differ — matches the original exercise), **`ToleranceMatchStrategy`** (treats prices as equal when \|A−B\| ≤ `reconciliation.price-tolerance`; tolerance ≤ 0 behaves like exact), and **`PriorityBrokerStrategy`** (strict compare; unified row follows **`reconciliation.primary-broker`**). `TradeReconciliationService` selects the implementation from `ReconciliationConfig`. |
| **Externalized configuration** | Classpath **`src/main/resources/application.properties`**: `reconciliation.strategy` (`exact` \| `tolerance` \| `priority-broker`), `reconciliation.price-tolerance`, `reconciliation.primary-broker` (`A` \| `B`), `trade.feed.path.a` / `trade.feed.path.b`, `financial.price.scale`, `financial.price.rounding-mode` (e.g. `HALF_UP`). **Env overrides** (highest precedence): `RECONCILIATION_STRATEGY`, `RECONCILIATION_PRICE_TOLERANCE`, `RECONCILIATION_PRIMARY_BROKER`, `TRADE_FEED_A`, `TRADE_FEED_B`, `FINANCIAL_PRICE_SCALE`, `FINANCIAL_PRICE_ROUNDING_MODE`. CLI still accepts **two path arguments** when provided; they override only the two feed paths for that run. |
| **Financial precision** | **`BigDecimal`** for quantity and price in `TradeData`; conflict diff strings and unified CSV price formatting use **`TradeAmountFormatter`** with the configured **scale** and **`RoundingMode`** so rounding is explicit and audit-friendly (not `double`). |

## Packages

| Package | Types |
|---------|--------|
| `com.oaktree.reconciliation` | `TradeReconciliationMain` |
| `com.oaktree.reconciliation.config` | `ReconciliationConfig`, `ReconciliationConfigLoader`, `ReconciliationStrategyType` |
| `com.oaktree.reconciliation.strategy` | `ReconciliationStrategy`, `AbstractReconciliationStrategy`, `ExactMatchStrategy`, `ToleranceMatchStrategy`, `PriorityBrokerStrategy` |
| `com.oaktree.reconciliation.model` | `TradeData`, `Broker`, `RejectedRecord`, `FieldConflict`, `ReconciliationResult`, `ReconciliationSummary` |
| `com.oaktree.reconciliation.io` | `TradeCsvParser` |
| `com.oaktree.reconciliation.service` | `TradeReconciliationService` |
| `com.oaktree.reconciliation.util` | `TradeValidator`, `TradeAmountFormatter` |
| `com.oaktree.reconciliation.report` | `ReconciliationReportPrinter` |
