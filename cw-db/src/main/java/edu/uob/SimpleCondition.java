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
        if (actualValue.startsWith("'") && actualValue.endsWith("'")) {
            actualValue = actualValue.substring(1, actualValue.length() - 1);
        }
        switch (comparator) {
            case "==":
                // Handle quoted values by removing the quotes
                String compareValue = value;
                if (value.startsWith("'") && value.endsWith("'")) {
                    compareValue = value.substring(1, value.length() - 1);
                }
                return actualValue.equals(compareValue);
            case "!=":
                String neqValue = value;
                if (value.startsWith("'") && value.endsWith("'")) {
                    neqValue = value.substring(1, value.length() - 1);
                }
                return !actualValue.equals(neqValue);
            case ">":
                try {
                    return Double.parseDouble(actualValue) > Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return actualValue.compareTo(value) > 0; // Fallback to string comparison
                }
            case "<":
                try {
                    return Double.parseDouble(actualValue) < Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return actualValue.compareTo(value) < 0;
                }
            case ">=":
                try {
                    return Double.parseDouble(actualValue) >= Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return actualValue.compareTo(value) >= 0;
                }
            case "<=":
                try {
                    return Double.parseDouble(actualValue) <= Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return actualValue.compareTo(value) <= 0;
                }
            case "LIKE":
                // Handle quoted patterns
                String pattern = value;
                if (pattern.startsWith("'") && pattern.endsWith("'")) {
                    pattern = pattern.substring(1, pattern.length() - 1);
                }
                // Convert SQL LIKE pattern to regex pattern
                pattern = "^" + pattern.replace("%", ".*").replace("_", ".") + "$";
                return actualValue.matches(pattern);
            default:
                return false;
        }
    }
}
