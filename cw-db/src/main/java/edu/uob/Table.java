package edu.uob;

import java.io.BufferedWriter;
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
    public void setData(List<List<String>> data){
        this.data = data;
    }
    public List<List<String>> getData()
    {
        return data;
    }
    public List<List<String>> convertMapToTableData(List<Map<String, String>> mappedRows) {
        List<List<String>> tableData = new ArrayList<>();
        int idCounter = 1; // Start ID from 1

        for (Map<String, String> rowMap : mappedRows) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(idCounter)); // Add the ID as the first column

            for (String column : colNames) {
                if (!column.equalsIgnoreCase("id")) { // Ensure "id" is not duplicated
                    row.add(rowMap.getOrDefault(column, "")); // Ensure all columns exist
                }
            }

            tableData.add(row);
            idCounter++; // Increment ID for the next row
        }

        return tableData;
    }

    public List<Map<String, String>> convertTableDataToMap(List<List<String>> data, List<String> columns) {
        List<Map<String, String>> mappedRows = new ArrayList<>();

        for (List<String> row : data) {
            if (row.isEmpty()) continue;

            Map<String, String> rowMap = new HashMap<>();
            List<String> values = row;

            // Process each column that has a corresponding value
            for (int i = 0; i < columns.size() && i < values.size(); i++) {
                rowMap.put(columns.get(i), values.get(i));
            }

            // Fill missing values with empty strings
            for (int i = values.size(); i < columns.size(); i++) {
                rowMap.put(columns.get(i), "");
            }

            mappedRows.add(rowMap);
        }

        return mappedRows;
    }



    public String getName() {
        return tableName;
    }
    public List<String> getColumns() {
        return colNames;
    }
    public List<List<String>> getRows(){
        return data;
    }
    public void addColumn(String attributeName) {
        // Check if column already exists
        if (colNames.contains(attributeName)) {
            System.out.println("Error: Column '" + attributeName + "' already exists");
            return;
        }

        // Add the column name to the list
        this.colNames.add(attributeName);
        this.totalColumns = this.colNames.size();

        // Add NULL values for the new column in each existing row
        for (List<String> row : data) {
            row.add("NULL");
        }

        // Now update the .tab file with the modified data
        try {
            File dbFolder = new File(storageFolderPath + "/" + databaseName);
            File tableFile = new File(dbFolder, tableName + ".tab");

            // Overwrite the file with the updated data
            try (FileWriter writer = new FileWriter(tableFile, false)) {
                // Write the header row (column names)
                for (int i = 0; i < colNames.size(); i++) {
                    writer.write(colNames.get(i));
                    if (i < colNames.size() - 1) {
                        writer.write("\t");
                    }
                }
                writer.write("\n");

                // Write each data row
                for (List<String> row : data) {
                    for (int i = 0; i < row.size(); i++) {
                        writer.write(row.get(i));
                        if (i < row.size() - 1) {
                            writer.write("\t");
                        }
                    }
                    writer.write("\n");
                }

                System.out.println("Column '" + attributeName + "' added to table file: " + tableFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("Error updating table file after adding column: " + e.getMessage());
        }

        System.out.println("Column '" + attributeName + "' added to table: " + tableName);
    }
    public void deleteColumn(String attributeName) {
        // Find the index of the column to delete
        int columnIndex = colNames.indexOf(attributeName);

        // If column doesn't exist or is the ID column (which cannot be deleted), return
        if (columnIndex == -1) {
            System.out.println("Error: Column '" + attributeName + "' does not exist");
            return;
        }

        if (columnIndex == 0 && attributeName.equals("id")) {
            System.out.println("Error: Cannot delete the 'id' column");
            return;
        }

        // Remove the column name
        colNames.remove(attributeName);
        this.totalColumns = this.colNames.size();

        // Remove the column data from each row
        for (List<String> row : data) {
            if (columnIndex < row.size()) {
                row.remove(columnIndex);
            }
        }

        // Now update the .tab file with the modified data
        try {
            File dbFolder = new File(storageFolderPath + "/" + databaseName);
            File tableFile = new File(dbFolder, tableName + ".tab");

            // Overwrite the file with the updated data
            try (FileWriter writer = new FileWriter(tableFile, false)) {
                // Write the header row (column names)
                for (int i = 0; i < colNames.size(); i++) {
                    writer.write(colNames.get(i));
                    if (i < colNames.size() - 1) {
                        writer.write("\t");
                    }
                }
                writer.write("\n");

                // Write each data row
                for (List<String> row : data) {
                    for (int i = 0; i < row.size(); i++) {
                        writer.write(row.get(i));
                        if (i < row.size() - 1) {
                            writer.write("\t");
                        }
                    }
                    writer.write("\n");
                }

                System.out.println("Column '" + attributeName + "' deleted from table file: " + tableFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("Error updating table file after column deletion: " + e.getMessage());
        }

        System.out.println("Column '" + attributeName + "' deleted from table: " + tableName);
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
            System.out.println("size mismatch");
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
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true))) {
                // Write the ID first
                writer.write(id);

                // Write each value separated by tab
                for (String value : values) {
                    writer.write("\t" + value);
                }

                // Add a newline at the end of the row
                writer.write("\n");

                // Force flush to ensure data is written
                writer.flush();

                System.out.println("Row successfully added to table file");
            }
        } catch (IOException e) {
            System.out.println("Error writing to table file: " + e.getMessage());
        }

        System.out.println("Added row to table " + tableName + ": " + row);
    }
    public String displayColumns(ArrayList<String> requestedColumns) {
        StringBuilder result = new StringBuilder();

        // If the list is empty or null, return
        if (requestedColumns == null || requestedColumns.isEmpty()) {
            return "[ERROR]No columns specified for display";
        }

        // Special case: if the first element is "*", display all columns
        if (requestedColumns.size() == 1 && requestedColumns.get(0).equals("*")) {
            displayTableDetails();
            return toString();
        }

        // Find the indices of the requested columns
        ArrayList<Integer> columnIndices = new ArrayList<>();
        for (String colName : requestedColumns) {
            int index = colNames.indexOf(colName);
            if (index != -1) {
                columnIndices.add(index);
            } else {
                System.out.println("Warning: Column '" + colName + "' does not exist in table");
            }
        }

        // If no valid columns found, return
        if (columnIndices.isEmpty()) {
            return "[ERROR]None of the requested columns exist in the table";
        }

        // Add header to the result
        result.append("Table: ").append(tableName).append("\n");

        // Add column names to the result
        for (int i = 0; i < columnIndices.size(); i++) {
            int index = columnIndices.get(i);
            result.append(colNames.get(index));
            if (i < columnIndices.size() - 1) {
                result.append("\t");
            }
        }
        result.append("\n"); // New line after column headers

        // Add data for the selected columns to the result
        for (List<String> row : data) {
            for (int i = 0; i < columnIndices.size(); i++) {
                int colIndex = columnIndices.get(i);
                if (colIndex < row.size()) {
                    result.append(row.get(colIndex));
                } else {
                    result.append("NULL");
                }

                if (i < columnIndices.size() - 1) {
                    result.append("\t");
                }
            }
            result.append("\n"); // New line after each row
        }

        result.append("Total rows: ").append(totalRows);

        // Also print to console (keeping the original behavior)
//        System.out.println(result.toString());

        return  result.toString();
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
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Table Name: ").append(tableName).append("\n");
        sb.append("Columns: ").append(colNames).append("\n");
        sb.append("Total Columns: ").append(totalColumns).append("\n");
        sb.append("Total Rows: ").append(totalRows).append("\n");
        sb.append("Data:\n");

        for (List<String> row : data) {
            sb.append(row).append("\n");
        }

        return sb.toString();
    }
}