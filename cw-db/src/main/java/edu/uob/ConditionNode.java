package edu.uob;


import java.util.*;
class ConditionNode {
    String attribute;
    String comparator;
    String value;
    ConditionNode left, right;
    String boolOperator;

    ConditionNode(String attribute, String comparator, String value) {
        this.attribute = attribute;
        this.comparator = comparator;
        this.value = value;
    }

    ConditionNode(ConditionNode left, String boolOperator, ConditionNode right) {
        this.left = left;
        this.boolOperator = boolOperator;
        this.right = right;
    }
}