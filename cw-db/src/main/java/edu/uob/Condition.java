package edu.uob;

import java.util.Map;

public interface Condition {
    boolean evaluate(Map<String, String> row);
}