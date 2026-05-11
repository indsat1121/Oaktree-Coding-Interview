import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReconciliationResult {

    private final List<RejectedRecord> rejectedRecords;
    private final List<FieldConflict> conflicts;
    private final List<Trade_Data> unifiedTrades;
    private final ReconciliationSummary summary;

    public ReconciliationResult(
            List<RejectedRecord> rejectedRecords,
            List<FieldConflict> conflicts,
            List<Trade_Data> unifiedTrades,
            ReconciliationSummary summary) {
        this.rejectedRecords = Collections.unmodifiableList(new ArrayList<>(rejectedRecords));
        this.conflicts = Collections.unmodifiableList(new ArrayList<>(conflicts));
        this.unifiedTrades = Collections.unmodifiableList(new ArrayList<>(unifiedTrades));
        this.summary = summary;
    }

    public List<RejectedRecord> getRejectedRecords() {
        return rejectedRecords;
    }

    public List<FieldConflict> getConflicts() {
        return conflicts;
    }

    public List<Trade_Data> getUnifiedTrades() {
        return unifiedTrades;
    }

    public ReconciliationSummary getSummary() {
        return summary;
    }
}
