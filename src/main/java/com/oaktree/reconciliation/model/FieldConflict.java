package com.oaktree.reconciliation.model;

public class FieldConflict {

    private final String tradeId;
    private final String fieldName;
    private final String valueA;
    private final String valueB;

    public FieldConflict(String tradeId, String fieldName, String valueA, String valueB) {
        this.tradeId = tradeId;
        this.fieldName = fieldName;
        this.valueA = valueA;
        this.valueB = valueB;
    }

    public String getTradeId() {
        return tradeId;
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
