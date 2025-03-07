package edu.uob;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;


class Table {
    String databaseName;
    List<String> colNames;
    int totalColumns;
    int totalRows;
    String tableName;
    List<List<String>> data;
    String storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
    public Table(String tableName)
    {
        this.tableName = tableName;
        this.colNames = new ArrayList<>();
        this.totalRows = 0;
        this.data = new ArrayList<>();
        this.colNames.add("id");
        this.totalColumns = 1;
    }
    public Table(String tableName, ArrayList<String> colNames, String databaseName) {
        this.tableName = tableName;
        this.colNames = new ArrayList<>();
        this.totalRows = 0;
        this.data = new ArrayList<>();
        this.colNames.add("id");
        this.colNames.addAll(colNames);
        this.totalColumns = colNames.size();
        this.databaseName = databaseName;
    }
    public String getName() {
        return tableName;
    }
    public void addColumns(ArrayList<String> columns) {
        // We'll assume "id" is already added as the first column
        this.colNames.addAll(columns);
        this.totalColumns = this.colNames.size();
    }
    public void addValues(String tableName, ArrayList<String> values) {
        // Check if this is for the right table
        if (!this.tableName.equals(tableName)) {
            System.out.println("Error: Table name mismatch");
            return;
        }

        // Generate an ID for the new row (assuming auto-increment)
        String id = String.valueOf(++this.totalRows);

        // Create a new row with the ID as the first value
        List<String> row = new ArrayList<>();
        row.add(id);

        // Add the rest of the values
        row.addAll(values);

        // If row size doesn't match column count, log a warning
        if (row.size() != this.totalColumns) {
            System.out.println("Warning: Number of values doesn't match number of columns");
        }

        // Add the row to the in-memory data
        this.data.add(row);

        // Now, append the row to the .tab file
        try {
            // Assuming storage path and database name are accessible
            // You might need to pass these as parameters or make them class variables
            File dbFolder = new File(storageFolderPath + "/" + databaseName);
            File tableFile = new File(dbFolder, tableName + ".tab");

            // Append to the file using FileWriter with append flag set to true
            try (FileWriter writer = new FileWriter(tableFile, true)) {
                // Write the ID first
                writer.write(id);

                // Write each value separated by tab
                for (String value : values) {
                    writer.write("\t" + value);
                }

                // Add a newline at the end of the row
                writer.write("\n");

                System.out.println("Row added to table file: " + tableFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("Error writing to table file: " + e.getMessage());
        }

        System.out.println("Added row to table " + tableName + ": " + row);
    }
    public void displayTableDetails() {
        System.out.println("Table Name: " + tableName);
        System.out.println("Columns: " + colNames);
        System.out.println("Total Columns: " + totalColumns);
        System.out.println("Total Rows: " + totalRows);
        // Display the actual data
        System.out.println("Data:");
        for (List<String> row : data) {
            System.out.println(row);
        }
    }
}