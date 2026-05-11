import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class TradeReconciliationService {

    private static final double EPS = 1e-6;

    private TradeReconciliationService() {
    }

    public static ReconciliationResult reconcile(TradeCsvParser.LoadResult loadA, TradeCsvParser.LoadResult loadB) {
        Map<String, Trade_Data> mapA = loadA.validByTradeId();
        Map<String, Trade_Data> mapB = loadB.validByTradeId();

        Set<String> matchedIds = new HashSet<>(mapA.keySet());
        matchedIds.retainAll(mapB.keySet());

        List<FieldConflict> conflicts = new ArrayList<>();
        List<String> sortedMatched = new ArrayList<>(matchedIds);
        Collections.sort(sortedMatched);
        for (String id : sortedMatched) {
            conflicts.addAll(compareFields(id, mapA.get(id), mapB.get(id)));
        }

        Set<String> idsInFileA = loadA.getTradeIdsAppearingInFile();
        Set<String> idsInFileB = loadB.getTradeIdsAppearingInFile();

        Set<String> onlyA = new HashSet<>();
        for (String id : mapA.keySet()) {
            if (!idsInFileB.contains(id)) {
                onlyA.add(id);
            }
        }

        Set<String> onlyB = new HashSet<>();
        for (String id : mapB.keySet()) {
            if (!idsInFileA.contains(id)) {
                onlyB.add(id);
            }
        }

        List<Trade_Data> unified = new ArrayList<>();
        TreeMap<String, Trade_Data> unifiedById = new TreeMap<>();
        for (String id : matchedIds) {
            unifiedById.put(id, mapA.get(id));
        }
        for (String id : onlyA) {
            unifiedById.put(id, mapA.get(id));
        }
        for (String id : onlyB) {
            unifiedById.put(id, mapB.get(id));
        }
        unified.addAll(unifiedById.values());

        List<RejectedRecord> rejected = new ArrayList<>();
        rejected.addAll(loadA.getRejected());
        rejected.addAll(loadB.getRejected());

        ReconciliationSummary summary = new ReconciliationSummary(
                loadA.getTotalDataRows(),
                loadA.getRejected().size(),
                loadA.getValidTrades().size(),
                loadB.getTotalDataRows(),
                loadB.getRejected().size(),
                loadB.getValidTrades().size(),
                matchedIds.size(),
                conflicts.size(),
                onlyA.size(),
                onlyB.size());

        return new ReconciliationResult(rejected, conflicts, unified, summary);
    }

    private static List<FieldConflict> compareFields(String tradeId, Trade_Data a, Trade_Data b) {
        List<FieldConflict> out = new ArrayList<>();
        if (!safeEquals(a.getSymbol(), b.getSymbol())) {
            out.add(new FieldConflict(tradeId, "symbol", a.getSymbol(), b.getSymbol()));
        }
        if (!safeEquals(a.getSide(), b.getSide())) {
            out.add(new FieldConflict(tradeId, "side", a.getSide(), b.getSide()));
        }
        if (!sameDouble(a.getQuantity(), b.getQuantity())) {
            out.add(new FieldConflict(tradeId, "quantity", formatQuantity(a.getQuantity()), formatQuantity(b.getQuantity())));
        }
        if (!sameDouble(a.getPrice(), b.getPrice())) {
            out.add(new FieldConflict(tradeId, "price", formatPrice(a.getPrice()), formatPrice(b.getPrice())));
        }
        if (!java.util.Objects.equals(a.getTrade_date(), b.getTrade_date())) {
            out.add(new FieldConflict(tradeId, "trade_date", String.valueOf(a.getTrade_date()), String.valueOf(b.getTrade_date())));
        }
        if (!java.util.Objects.equals(a.getSettlement_date(), b.getSettlement_date())) {
            out.add(new FieldConflict(tradeId, "settlement_date", String.valueOf(a.getSettlement_date()), String.valueOf(b.getSettlement_date())));
        }
        if (!safeEquals(a.getAccount_id(), b.getAccount_id())) {
            out.add(new FieldConflict(tradeId, "account_id", a.getAccount_id(), b.getAccount_id()));
        }
        return out;
    }

    private static boolean safeEquals(String x, String y) {
        if (x == null && y == null) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }

    private static boolean sameDouble(double x, double y) {
        return Math.abs(x - y) < EPS * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
    }

    static String formatPrice(double p) {
        return String.format(Locale.US, "%.2f", p);
    }

    static String formatQuantity(double q) {
        if (Math.abs(q - Math.rint(q)) < EPS) {
            return String.valueOf((long) Math.rint(q));
        }
        return String.format(Locale.US, "%s", q);
    }
}
