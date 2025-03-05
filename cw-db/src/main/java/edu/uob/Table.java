package edu.uob;

import java.util.*;


class Table {
    List<String> colNames;
    int totalColumns;
    int totalRows;
    String tableName;
    public Table(String tableName)
    {
        this.tableName = tableName;
        this.colNames = new ArrayList<>();
        this.totalColumns = 0;
        this.totalRows = 0;
    }
    public Table(List<String> colNames, int totalRows) {
        this.colNames = new ArrayList<>(colNames);
        this.totalColumns = colNames.size();
        this.totalRows = totalRows;
    }
    public String getName()
    {
        return tableName;
    }
    public void displayTableDetails() {
        System.out.println("Columns: " + colNames);
        System.out.println("Total Columns: " + totalColumns);
        System.out.println("Total Rows: " + totalRows);
    }
}