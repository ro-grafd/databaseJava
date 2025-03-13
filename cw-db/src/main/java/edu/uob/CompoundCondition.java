package edu.uob;
import java.util.Map;

public class CompoundCondition implements Condition {
    private Condition left;
    private Condition right;
    private String operator;

    public CompoundCondition(Condition left, String operator, Condition right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public boolean evaluate(Map<String, String> row) {
        if (operator.equalsIgnoreCase("AND")) {
            return left.evaluate(row) && right.evaluate(row);
        } else if (operator.equalsIgnoreCase("OR")) {
            return left.evaluate(row) || right.evaluate(row);
        }
        return false;
    }
}
