package edu.uob;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionParser {
    public static Condition parse(String whereClause) {
        whereClause = whereClause.trim();
        if (whereClause.contains(" AND ") || whereClause.contains(" OR ")) {
            String[] parts;
            String operator;
            if (whereClause.contains(" AND ")) {
                parts = whereClause.split(" AND ", 2);
                operator = "AND";
            } else {
                parts = whereClause.split(" OR ", 2);
                operator = "OR";
            }
            return new CompoundCondition(parse(parts[0].trim()), operator, parse(parts[1].trim()));
        }
        Pattern pattern = Pattern.compile("([a-zA-Z0-9_]+)\\s*(==|!=|>=|<=|>|<|LIKE)\\s*(['\"]?)(.*?)\\3");
        Matcher matcher = pattern.matcher(whereClause);
        if (matcher.matches()) {
            return new SimpleCondition(matcher.group(1), matcher.group(2), matcher.group(4));
        }
        return null;
    }
}
