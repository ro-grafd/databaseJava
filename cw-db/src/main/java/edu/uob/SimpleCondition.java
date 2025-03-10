package edu.uob;
import java.util.Map;
public class SimpleCondition implements Condition {
    private String attribute;
    private String comparator;
    private String value;

    public SimpleCondition(String attribute, String comparator, String value) {
        this.attribute = attribute;
        this.comparator = comparator;
        this.value = value;
    }

    @Override
    public boolean evaluate(Map<String, String> row) {
        String actualValue = row.get(attribute);
        if (actualValue == null) return false;

        switch (comparator) {
            case "==": return actualValue.equals(value);
            case "!=": return !actualValue.equals(value);
            case ">": return Double.parseDouble(actualValue) > Double.parseDouble(value);
            case "<": return Double.parseDouble(actualValue) < Double.parseDouble(value);
            case ">=": return Double.parseDouble(actualValue) >= Double.parseDouble(value);
            case "<=": return Double.parseDouble(actualValue) <= Double.parseDouble(value);
            case "LIKE": return actualValue.matches(value.replace("%", ".*")); // Simple LIKE implementation
            default: return false;
        }
    }
}
