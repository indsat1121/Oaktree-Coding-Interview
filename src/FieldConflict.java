public class FieldConflict {

    private final String trade_id;
    private final String fieldName;
    private final String valueA;
    private final String valueB;

    public FieldConflict(String trade_id, String fieldName, String valueA, String valueB) {
        this.trade_id = trade_id;
        this.fieldName = fieldName;
        this.valueA = valueA;
        this.valueB = valueB;
    }

    public String getTrade_id() {
        return trade_id;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getValueA() {
        return valueA;
    }

    public String getValueB() {
        return valueB;
    }
}
