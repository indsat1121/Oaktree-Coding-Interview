# Use ./scripts/mvn so a JDK with javac is selected on macOS when JAVA_HOME points at a JRE.
MVN := ./scripts/mvn

.PHONY: compile test run clean

compile:
	$(MVN) -q compile

test:
	$(MVN) -q test

run: compile
	java -cp target/classes com.oaktree.reconciliation.TradeReconciliationMain

clean:
	$(MVN) -q clean
	rm -rf bin
