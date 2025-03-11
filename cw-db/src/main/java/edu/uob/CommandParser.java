package edu.uob;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.*;

public class CommandParser {
    private String currentDatabase;
    private final DataBaseSupreme dataBaseSupreme;
    private String storagePath;
    public CommandParser(DataBaseSupreme dataBaseSupreme, String storagePath) {
        this.storagePath = storagePath;
        this.dataBaseSupreme = dataBaseSupreme;
    }

    public void setCurrentDatabase(String currentDatabase) {
        this.currentDatabase = currentDatabase;
    }

    public String parseQuery(ArrayList<String> tokens) {
        // Ensure tokens are properly processed
        if (tokens.size() >= 3 && tokens.get(0).equalsIgnoreCase("CREATE") && tokens.get(1).equalsIgnoreCase("DATABASE") && tokens.get(tokens.size()-1).equals(";")) {
            return handleCreateDatabase(tokens.get(2));
        } else if (tokens.size() >= 2 && tokens.get(0).equalsIgnoreCase("USE") && tokens.get(tokens.size()-1).equals(";")) {
            return handleUseDatabase(tokens);
        } else if((tokens.size() >=3 && tokens.get(0).equalsIgnoreCase("CREATE") && tokens.get(1).equalsIgnoreCase("TABLE") && tokens.get(tokens.size()-1).equals(";") )||( tokens.size() >=3 && tokens.get(1).equalsIgnoreCase("CREATE") && tokens.get(2).equalsIgnoreCase("TABLE") && tokens.get(3).equals("(") && tokens.get(tokens.size()-2).equals(")") && tokens.get(tokens.size()-1).equals(";") )) {
            return handleCreateTable(tokens);
        } else if(tokens.size() == 4 && tokens.get(0).equalsIgnoreCase("DROP") && tokens.get(1).equalsIgnoreCase("DATABASE") && tokens.get(tokens.size()-1).equals(";")) {
            return handleDropDatabase(tokens);
        } else if(tokens.size() == 4 && tokens.get(0).equalsIgnoreCase("DROP") && tokens.get(1).equalsIgnoreCase("TABLE") && tokens.get(tokens.size()-1).equals(";")) {
            return handleDropTable(tokens);
        } else if(tokens.size() >= 6 && tokens.get(0).equalsIgnoreCase("INSERT") && tokens.get(1).equalsIgnoreCase("INTO") && tokens.get(3).equalsIgnoreCase("VALUES") && tokens.get(4).equals("(") && tokens.get(tokens.size()-2).equals(")") && tokens.get(tokens.size()-1).equals(";")) {
            return handleInsert(tokens);
        } else if(tokens.size() == 6 && tokens.get(0).equalsIgnoreCase("ALTER") && tokens.get(1).equalsIgnoreCase("TABLE") && tokens.get(tokens.size()-1).equals(";") ){
            return handleAlterTable(tokens);
        } else if(tokens.size() >= 5 && tokens.get(0).equalsIgnoreCase("SELECT")  && tokens.get(tokens.size()-1).equals(";")) {
            return handleSelect(tokens);
        } else if(tokens.get(0).equalsIgnoreCase("UPDATE") && tokens.get(2).equalsIgnoreCase("SET") && tokens.get(tokens.size()-1).equals(";")) {
            return handleUpdate(tokens);
        } else if(tokens.get(0).equalsIgnoreCase("DELETE") && tokens.get(1).equalsIgnoreCase("FROM") && tokens.get(tokens.size()-1).equals(";") ) {
            return handleDelete(tokens);
        } else if(tokens.size() == 9 && tokens.get(0).equalsIgnoreCase("JOIN") && tokens.get(2).equalsIgnoreCase("AND")  && tokens.get(4).equalsIgnoreCase("ON") && tokens.get(6).equalsIgnoreCase("AND") && tokens.get(8).equalsIgnoreCase(";") ) {
            return handleJoin(tokens);
        }
        // Handle other types of commands (if any) here
        return "Invalid command!";
    }
    private String handleJoin(ArrayList<String> tokens) {
        String tb1 = tokens.get(1);
        String tb2 = tokens.get(3);
        String at1 = tokens.get(5);
        String at2 = tokens.get(7);
        Database db = dataBaseSupreme.getCurrentDatabase();
        Table table1 = getTableFromName(tb1);
        Table table2 = getTableFromName(tb2);
        // Get the tables from your data structure (assuming you have tables stored somewhere)
        List<Map<String, String>> tableMap1 = convertDataToListMap(table1.data,table1.getColumns());
        List<Map<String, String>> tableMap2 = convertDataToListMap(table2.data, table2.getColumns());

        // Create a list to store the joined records
        List<Map<String, String>> joinedTable = new ArrayList<>();

        // Perform the join operation
        for (Map<String, String> row1 : tableMap1) {
            for (Map<String, String> row2 : tableMap2) {
                // Check if join attributes match
                if (row1.containsKey(at1) && row2.containsKey(at2) &&
                        row1.get(at1).equals(row2.get(at2))) {

                    // Create a new map for the joined row
                    Map<String, String> joinedRow = new HashMap<>();

                    // Add all columns from table1 except the join attribute
                    for (Map.Entry<String, String> entry : row1.entrySet()) {
                        if (!entry.getKey().equals(at1)) {
                            joinedRow.put(tb1 + "." + entry.getKey(), entry.getValue());
                        }
                    }

                    // Add all columns from table2 except the join attribute
                    for (Map.Entry<String, String> entry : row2.entrySet()) {
                        if (!entry.getKey().equals(at2)) {
                            joinedRow.put(tb2 + "." + entry.getKey(), entry.getValue());
                        }
                    }

                    // Add the joined row to our result
                    joinedTable.add(joinedRow);
                }
            }
        }

        // Return the string representation of the joined table
        return joinedTable.toString();
    }
    private List<Map<String, String>> convertDataToListMap(List<List<String>> data, List<String> columns) {
        List<Map<String, String>> result = new ArrayList<>();

        // Check if data is empty or if columns are not provided
        if (data.isEmpty() || columns.isEmpty()) {
            return result;
        }

        // Process each row in the data
        for (List<String> row : data) {
            Map<String, String> rowMap = new HashMap<>();

            // Map each column value to its corresponding header
            for (int j = 0; j < columns.size() && j < row.size(); j++) {
                rowMap.put(columns.get(j), row.get(j));
            }

            result.add(rowMap);
        }

        return result;
    }


    private String handleDelete(ArrayList<String> tokens) {
        if (tokens.size() < 4 || !tokens.get(1).equalsIgnoreCase("FROM")) {
            return "[ERROR]: Invalid DELETE query syntax!";
        }

        String tableName = tokens.get(2);
        Database db = dataBaseSupreme.getCurrentDatabase();
        if (db == null) {
            return "[ERROR]: Database not set!";
        }

        // Check if the table exists in memory, otherwise load from the file system
        if (!db.searchTable(tableName)) {
            File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
            if (!tableFile.exists()) {
                return "[ERROR]: Table " + tableName + " does not exist!";
            }
            db.loadTableFromFile(tableFile); // Load table into memory
        }

        // Get the table from memory
        Table table = db.getTable(tableName);
        if (table == null) {
            return "[ERROR]: Failed to retrieve table!";
        }

        // Extract WHERE clause
        String whereClause = extractWhereClause(tokens);
        Condition condition = whereClause.isEmpty() ? null : ConditionParser.parse(whereClause);

        // Convert table data into map representation
        List<Map<String, String>> tableData = table.convertTableDataToMap(table.getData(), table.getColumns());

        // Filter out rows that satisfy the condition
        List<Map<String, String>> updatedData = new ArrayList<>();
        int deletedCount = 0;

        for (Map<String, String> row : tableData) {
            if (condition == null || !condition.evaluate(row)) {
                updatedData.add(row); // Keep row if it does not satisfy the condition
            } else {
                deletedCount++;
            }
        }

        if (deletedCount == 0) {
            return "[ERROR]: No matching rows found!";
        }

        // Convert updated data back to List<List<String>>
        List<List<String>> updatedTableData = table.convertMapToTableData(updatedData);
        table.setData(updatedTableData);

        // Save updated table back to file system
        try {
            updateTableFile(new File(storagePath + "/" + db.getName(), tableName + ".tab"), table);
        } catch (IOException e) {
            return "[ERROR]: Failed to update table file!";
        }

        return "[OK] Deleted " + deletedCount + " rows from '" + tableName + "'.";
    }

    private String handleUpdate(ArrayList<String> tokens) {
        if (tokens.size() < 6 || !tokens.get(2).equalsIgnoreCase("SET")) {
            return "[ERROR]: Invalid UPDATE query syntax!";
        }

        String tableName = tokens.get(1);
        Database db = dataBaseSupreme.getCurrentDatabase();
        if(db == null) {
            return "[ERROR]: Database not set!";
        }
        // Check if the database exists in memory, if not, load from the file system
        if (!db.searchTable(tableName)) {
            File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
            if (!tableFile.exists()) {
                return "[ERROR]: Table " + tableName + " does not exist!";
            }
            db.loadTableFromFile(tableFile); // Load table into memory
        }

        // Get the table from memory
        Table table = db.getTable(tableName);
        if (table == null) {
            return "[ERROR]: Failed to retrieve table!";
        }

        // Extract the SET clause
        int whereIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("WHERE")) {
                whereIndex = i;
                break;
            }
        }
        if (whereIndex == -1) {
            return "[ERROR]: Missing WHERE clause!";
        }

        List<String> setClauseTokens = tokens.subList(3, whereIndex);
        Map<String, String> updates = extractSetClause(setClauseTokens);

        // Extract the WHERE clause
        String whereClause = extractWhereClause(tokens);
        Condition condition = whereClause.isEmpty() ? null : ConditionParser.parse(whereClause);
        // Update rows that satisfy the condition
        List<Map<String, String>> tableData = table.convertTableDataToMap(table.getData(), table.getColumns());
        boolean rowUpdated = false;

        for (Map<String, String> row : tableData) {
            if (condition == null || condition.evaluate(row)) {
                for (Map.Entry<String, String> entry : updates.entrySet()) {
                    row.put(entry.getKey(), entry.getValue()); // Apply updates
                }
                rowUpdated = true;
            }
        }

        if (!rowUpdated) {
            return "[ERROR]: No matching rows found!";
        }

        // Convert updated data back to List<List<String>>
        List<List<String>> updatedTableData = table.convertMapToTableData(tableData);
        table.setData(updatedTableData);

        // Save table back to file system
        try {
            updateTableFile(new File(storagePath + "/" + db.getName(), tableName + ".tab"), table);
        } catch (IOException e) {
            return "[ERROR]: Failed to update table file!";
        }

        return "[OK] Table " + tableName + " updated successfully!";
    }
    private Map<String, String> extractSetClause(List<String> tokens) {
        Map<String, String> updates = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals(",")) {
                continue; // Skip commas
            }
            if (i + 2 >= tokens.size() || !tokens.get(i + 1).equals("=")) {
                throw new IllegalArgumentException("Invalid SET clause format!");
            }
            updates.put(tokens.get(i), tokens.get(i + 2));
            i += 2; // Move to the next column-value pair
        }
        return updates;
    }

    private String handleSelect(ArrayList<String> tokens) {
        if(tokens.get(tokens.size()-3).equalsIgnoreCase("FROM")) {
            String tableName = tokens.get(tokens.size()-2);
            // Check if database is set
            if (dataBaseSupreme.getCurrentDatabase() == null) {
                return "[ERROR]Database not set";
            }
            Database db = dataBaseSupreme.getCurrentDatabase();
            // First check if table exists in memory
            boolean tableInMemory = db.searchTable(tableName);
            // Check filesystem if not in memory
            if (!tableInMemory) {
                // Path to the table file in the file system
                File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");

                // If table exists in filesystem but not in memory, load it
                if (tableFile.exists()) {
                    try {
                        // Load table from file
                        TableLoadResult result = loadTableFromFile(tableFile, tableName);

                        // Add it to the database
                        db.addTable(result.table, result.columns);

                        // Verify the table was added successfully
                        if (!db.searchTable(tableName)) {
                            return "[ERROR]: Failed to load table " + tableName + " into memory!";
                        }
                    } catch (Exception e) {
                        return "[ERROR] loading table from file: " + e.getMessage();
                    }
                } else {
                    // Table not found in memory or filesystem
                    return "[ERROR] Table " + tableName + " not found!";
                }
            }
            // At this point, the table should be in memory
            Table tb = db.getTable(tableName);
            ArrayList<String> columns = new ArrayList<>();
            getAttributes(tokens, columns);
            return tb.displayColumns(columns) + "[OK]";
        }else if(tokens.size() >= 5) {
            List<Map<String,String>> result;
            result = executeSelect(tokens);
            StringBuilder resultString = new StringBuilder();
            for (Map<String, String> row : result) {
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    resultString.append(entry.getKey())
                            .append(" ")
                            .append(entry.getValue())
                            .append(" ");
                }
                resultString.append("\n");
            }
            return resultString.toString();
        }
        return "[ERROR] some mishap with the select command!";
    }
    public List<Map<String, String>> executeSelect(ArrayList<String> tokens) {
        List<Map<String, String>> result = new ArrayList<>();
        int fromIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("FROM")) {
                fromIndex = i;
                break;
            }
        }
        // Extract table name from tokens
        String tableName = tokens.get(fromIndex + 1);
        Table table = getTableFromName(tableName);
//        for(String column : table.getColumns()) {
//            System.out.println(column + " <- index values");
//        }
        if (table == null) return result; // Table not found

        // Extract selected columns
        List<String> selectedColumns = extractSelectedColumns(tokens);

        // Extract WHERE clause
        String whereClause = extractWhereClause(tokens);
        Condition condition = whereClause.isEmpty() ? null : ConditionParser.parse(whereClause);
        List<Map<String, String>> tableRows = table.convertTableDataToMap(table.getRows(), table.getColumns());
        // Iterate through all rows and filter based on the condition
        for (Map<String, String> row : tableRows) {
            if (condition == null || condition.evaluate(row)) {
                Map<String, String> selectedRow = new HashMap<>();
                if(selectedColumns.get(0).equalsIgnoreCase("*")) {
                    for(String col : table.getColumns()) {
                        selectedRow.put(col, row.get(col));
                    }
                }else {
                    for (String col : selectedColumns) {
                        selectedRow.put(col, row.get(col));
                    }
                }
                result.add(selectedRow);
            }
        }

        return result;
    }
    private List<String> extractSelectedColumns(ArrayList<String> tokens) {
        List<String> selectedColumns = new ArrayList<>();
        int fromIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("FROM")) {
                fromIndex = i;
                break;
            }
        }
        for (int i = 1; i < fromIndex; i++) { // Columns are between SELECT and FROM
            if (!tokens.get(i).equals(",")) {
                selectedColumns.add(tokens.get(i));
            }
        }
        return selectedColumns;
    }
    private String extractWhereClause(ArrayList<String> tokens) {
        int whereIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("WHERE")) {
                whereIndex = i;
                break;
            }
        }
        if (whereIndex == -1) return ""; // No WHERE clause

        StringBuilder whereClause = new StringBuilder();
        for (int i = whereIndex + 1; i < tokens.size()-1; i++) {
            whereClause.append(tokens.get(i)).append(" ");
        }
        return whereClause.toString().trim();
    }

    private Table getTableFromName(String tableName) {
        Database db = dataBaseSupreme.getCurrentDatabase();
        if (db == null) return null;

        if (!db.searchTable(tableName)) {
            // Load from file system if not in memory
            File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
            if (tableFile.exists()) {
                db.loadTableFromFile(tableFile);
            } else {
                return null; // Table not found
            }
        }
        return db.getTable(tableName);
    }

    private void getAttributes(ArrayList<String> tokens, ArrayList<String> columns) {
        // Find the index of "SELECT" keyword
        int selectIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("SELECT")) {
                selectIndex = i;
                break;
            }
        }
        // If "SELECT" keyword not found, return empty list
        if (selectIndex == -1) {
            return;
        }
        // Find the index of "FROM" keyword
        int fromIndex = -1;
        for (int i = selectIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("FROM")) {
                fromIndex = i;
                break;
            }
        }
        // If "FROM" keyword not found, return empty list
        if (fromIndex == -1) {
            return;
        }
        // Collect all tokens between SELECT and FROM, skipping commas
        for (int i = selectIndex + 1; i < fromIndex; i++) {
            // Skip commas
            if (!tokens.get(i).equals(",")) {
                columns.add(tokens.get(i));
            }
        }
        // Handle the special case of SELECT *
        if (columns.size() == 1 && columns.get(0).equals("*")) {
            // Clear the list as we'll handle the * case differently
            // (typically by fetching all columns from the table elsewhere)
            System.out.println("SELECT * detected - all columns will be selected");
        }
    }
    private String handleAlterTable(ArrayList<String> tokens) {
        String tableName = tokens.get(2);
        // Check if database is set
        if (dataBaseSupreme.getCurrentDatabase() == null) {
            return "[ERROR]Database not set";
        }
        Database db = dataBaseSupreme.getCurrentDatabase();
        // Path to the table file in the file system
        File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
        // Check if the table file exists in the file system
        if (!tableFile.exists()) {
            return "[ERROR]: Table " + tableName + " does not exist in the file system!";
        }
        // Check if the table exists in memory (Database object)
        // If it doesn't exist in memory but exists in filesystem, load it into memory
        if (!db.searchTable(tableName)) {
            try {
                // Load table and columns from file
                TableLoadResult result = loadTableFromFile(tableFile, tableName);
                // Add the table to the database using both table and columns
                db.addTable(result.table, result.columns);
                // Confirm the table was successfully added to memory
                if (!db.searchTable(tableName)) {
                    return "[ERROR]: Failed to load table " + tableName + " into memory!";
                }
            } catch (Exception e) {
                return "[ERROR]: Failed to load table from file: " + e.getMessage();
            }
        }
        // Get the table from memory
        Table tb = db.getTable(tableName);
        // Determine the operation (ADD or DROP)
        String attribute = tokens.get(4);
        // Perform the operation: ADD or DROP
        if (tokens.get(3).equalsIgnoreCase("ADD")) {
            tb.addColumn(attribute);
            // After adding the column, update the table file
            try {
                updateTableFile(tableFile, tb);
            } catch (IOException e) {
                return "[ERROR]: Failed to update table file after adding column: " + e.getMessage();
            }
            return "[OK]Column " + attribute + " added!";
        } else if (tokens.get(3).equalsIgnoreCase("DROP")) {
            tb.deleteColumn(attribute);
            // After dropping the column, update the table file
            try {
                updateTableFile(tableFile, tb);
            } catch (IOException e) {
                return "[ERROR]: Failed to update table file after dropping column: " + e.getMessage();
            }
            return "[OK]Column " + attribute + " dropped!";
        }

        return "[ERROR]Invalid Alter command!";
    }

    // Helper class to return both table and columns from the load method
    private class TableLoadResult {
        Table table;
        ArrayList<String> columns;
        TableLoadResult(Table table, ArrayList<String> columns) {
            this.table = table;
            this.columns = columns;
        }
    }

    private TableLoadResult loadTableFromFile(File tableFile, String tableName) throws Exception {
        Table table = new Table(tableName);
        ArrayList<String> columns = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            // Read the header line (column names)
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new Exception("Table file is empty or corrupted");
            }

            // Parse column names (tab-delimited)
            String[] columnNames = headerLine.trim().split("\\s+");
            // Skip the first column if it's "id" as it's special
            int startIndex = 0;
            if (columnNames.length > 0 && columnNames[0].trim().equals("id")) {
                startIndex = 1;
            }

            // Add columns to both the table and the columns list
            for (int i = startIndex; i < columnNames.length; i++) {
                String columnName = columnNames[i].trim();
                if (!columnName.isEmpty()) {
                    table.addColumn(columnName);
                    columns.add(columnName);
                }
            }

            // Read data rows
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] values = line.split("\t");

                    // Create a new row
                    ArrayList<String> row = new ArrayList<>();

                    // Add values to the row, ensuring we don't exceed the number of columns
                    for (int i = startIndex; i < Math.min(values.length, columnNames.length); i++) {
                        row.add(values[i].trim());
                    }

                    // Add the populated row to the table
                    table.addValues(tableName,row);
                }
            }

            return new TableLoadResult(table, columns);
        } catch (IOException e) {
            throw new Exception("ERROR reading table file: " + e.getMessage(), e);
        }
    }

    private void updateTableFile(File tableFile, Table table) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile))) {
            // Write header (column names)
            List<String> columns = table.getColumns();

            // Always start with "id" column
//            writer.write("id");
            writer.write(" ");
            // Write the rest of the columns
            for (String column : columns) {
                writer.write(column + " ");
            }
            writer.newLine();

            // Write data rows
            List<List<String>> rows = table.getRows();
            for (List<String> row : rows) {
                // Since row is a List<String>, we can directly access elements by index
                // First element (index 0) is typically the ID
                if (row.size() > 0) {
                    writer.write(row.get(0));  // Write the ID

                    // Write the remaining values (starting from index 1)
                    for (int i = 1; i < row.size(); i++) {
                        String value = (i < row.size()) ? row.get(i) : "";
                        writer.write("\t" + value);
                    }
                }

                writer.newLine();
            }
        }
    }
    private String handleDropTable(ArrayList<String> tokens) {
        String tableName = tokens.get(2);
        // Check if the current database is set
        if (dataBaseSupreme.getCurrentDatabase() == null) {
            return "[ERROR]Database not set";
        }
        // Get the current database
        Database db = dataBaseSupreme.getCurrentDatabase();
        // Check if the database exists in-memory
        if (db == null) {
            return "[ERROR]Database " + dataBaseSupreme.getCurrentDatabase().getName() + " does not exist!";
        }
        // Check if the table exists in-memory
        if (db.searchTable(tableName)) {
            Table tb = db.getTable(tableName);
            db.deleteTable(tb);  // Delete the table both from memory and file system
            return "[OK]Table " + tableName + " has been dropped!";
        }
        // If the table is not found in memory, check the file system
        File dbFolder = new File(storagePath + "/" + db.getName());
        File tableFile = new File(dbFolder, tableName + ".tab");

        // If the table exists on the file system, delete it
        if (tableFile.exists() && tableFile.isFile()) {
            // Delete the table file from the file system
            boolean deleted = tableFile.delete();
            if (deleted) {
                System.out.println("Table file " + tableFile.getAbsolutePath() + " deleted.");
                return "[OK]Table " + tableName + " has been dropped from the file system!";
            } else {
                return "[ERROR]Failed to delete table file " + tableFile.getAbsolutePath();
            }
        }
        // If the table is not found in-memory or on the file system
        return "[ERROR]Table " + tableName + " does not exist!";
    }

    private String handleDropDatabase(ArrayList<String> tokens) {
        String toDeleteDatabase = tokens.get(2);
        Database db = dataBaseSupreme.getDatabase(toDeleteDatabase);
        if(db == null) {
            return "[ERROR]Database does not exist!";
        }
        File dbFolder = new File(storagePath, toDeleteDatabase);
        if (dbFolder.exists() && dbFolder.isDirectory()) {
            // Delete the database folder and its contents
            try {
                deleteDirectory(dbFolder);
                System.out.println("Database folder deleted from file system: " + dbFolder.getAbsolutePath());
            } catch (IOException e) {
                return "[ERROR] deleting database folder from file system: " + e.getMessage();
            }
        } else {
            return "[ERROR] Database folder not found in the file system.";
        }
        dataBaseSupreme.deleteDatabase(db);
        return "[OK]Database dropped!";
    }
    private void deleteDirectory(File folder) throws IOException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file); // Recursively delete subdirectories
                } else {
                    if (!file.delete()) {
                        throw new IOException("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
        if (!folder.delete()) {
            throw new IOException("Failed to delete folder: " + folder.getAbsolutePath());
        }
    }
    private String handleInsert(ArrayList<String> tokens) {
        Database db = dataBaseSupreme.getCurrentDatabase();
        // Check if a database is currently selected
        if (db == null) {
            return "[ERROR]No database is in use";
        }
        String tableName = tokens.get(2);
        // Check if the table exists in memory
        if (!db.searchTable(tableName)) {
            // Also check if the table file exists in the file system
            File tableFile = new File(storagePath, db.getName() + "/" + tableName + ".tab");

            if (!tableFile.exists()) {
                return "[ERROR]Table " + tableName + " does not exist in the database.";
            } else {
                // If table exists in the file system but not in memory, you may want to load it into memory.
                db.loadTableFromFile(tableFile);
            }
        }
        // Retrieve the table (now it should be in memory)
        Table tb = db.getTable(tableName);
        tb.databaseName = db.getName();
        if (tb == null) {
            return "[ERROR]: Unable to retrieve table " + tableName;
        }
        // Extract values
        ArrayList<String> values = new ArrayList<>();
        getValues(tokens, values);
        // Add values to the table
        if(values.size() + 1 != tb.colNames.size() ) {
            return "[ERROR]Table has" + (tb.totalColumns - 1) + " column(s)!";
        }
        tb.addValues(tableName, values);
        return "[OK]Inserted into " + tableName + " values " + values;
    }

    private void getValues(ArrayList<String> tokens, ArrayList<String> values) {
        int valuesIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("VALUES")) {
                valuesIndex = i;
                break;
            }
        }
        // If "VALUES" keyword not found, return empty list
        if (valuesIndex == -1) {
            return;
        }
        // Find the opening parenthesis that comes after "VALUES"
        int openParenIndex = -1;
        for (int i = valuesIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).equals("(")) {
                openParenIndex = i;
                break;
            }
        }
        // If no opening parenthesis found, return empty list
        if (openParenIndex == -1) {
            return;
        }
        // Start from the element after the opening parenthesis
        int i = openParenIndex + 1;
        // Continue until we hit the closing parenthesis
        while (i < tokens.size() && !tokens.get(i).equals(")")) {
            // Skip commas
            if (!tokens.get(i).equals(",")) {
                values.add(tokens.get(i));
            }
            i++;
        }
    }
    private String handleCreateTable(ArrayList<String> tokens) {
        Database db = dataBaseSupreme.getCurrentDatabase();

        if (db == null) {
            return "[ERROR]: No database is in use";
        }

        String tableName = tokens.get(2);

        // Check if the table already exists in memory
        if (db.searchTable(tableName)) {
            return "[ERROR]: Table already exists!";
        }

        // Check if the table file exists in the file system
        File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
        if (tableFile.exists()) {
            return "[ERROR]: Table file already exists in the file system!";
        }

        // Extract column names
        ArrayList<String> columns = new ArrayList<>();
        getColumns(tokens, columns);

        // Create and add the table
        Table tb = new Table(tableName,columns, db.getName());
        db.addTable(tb, columns);

        // Create the table file in the database directory
        try {
                tableFile.createNewFile();
                return "[OK]Table '" + tableName + "' created successfully!";

        } catch (IOException e) {
            return "[ERROR]: IOException occurred while creating the table file: " + e.getMessage();
        }

    }

    private void getColumns(ArrayList<String> tokens, ArrayList<String> columns)
    {
        int openParenIndex = tokens.indexOf("(");
        if(openParenIndex == -1) return;
        // Start from the element after the opening parenthesis
        int i = openParenIndex + 1;
        // Continue until we hit the closing parenthesis
        while (!tokens.get(i).equals(")")) {
            // Skip commas
            if (!tokens.get(i).equals(",")) {
                columns.add(tokens.get(i));
            }
            i++;
        }
    }
    private String handleCreateDatabase(String dbName) {
        // Check if the database already exists in memory
        if (dataBaseSupreme.searchDatabases(dbName)) {
            return "[ERROR]: Database already exists in memory.";
        }
        // Check if the database folder exists in the file system
        File dbFolder = new File(storagePath, dbName);
        if (dbFolder.exists() && dbFolder.isDirectory()) {
            return "[ERROR]: Database folder already exists in the file system.";
        }
        // Create the new database folder
        if (dbFolder.mkdirs()) {
            dataBaseSupreme.addDatabase(new Database(dbName, storagePath));
            return "[OK]Database '" + dbName + "' created successfully!";
        } else {
            return "[ERROR]: Failed to create database.";
        }
    }

    private String handleUseDatabase(ArrayList<String> tokens) {
        String dbName = tokens.get(1);
        // Check if already using the same database
        if (dataBaseSupreme.currentDatabase != null && dataBaseSupreme.currentDatabase.equals(dbName)) {
            return "Already in the same database";
        }
        // Check if the database exists in memory
        if (dataBaseSupreme.searchDatabases(dbName)) {
            dataBaseSupreme.setCurrentDatabase(dbName);
            return "[OK]Current database set to " + dbName;
        }
        // Check if the database exists in the file system
        File dbFolder = new File(storagePath, dbName);
        if (dbFolder.exists() && dbFolder.isDirectory()) {
            dataBaseSupreme.addDatabase(new Database(dbName, storagePath)); // Load the database
            dataBaseSupreme.setCurrentDatabase(dbName);
            return "[OK]Current database set to " + dbName;
        }
        return "[ERROR]: Database not found in memory or file system.";
    }
}
