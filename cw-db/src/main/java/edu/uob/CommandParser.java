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

    public String parseQuery(ArrayList<String> tokens) {
        // Ensure tokens are properly processed
        if (isCreateDatabaseStatement(tokens)) {
            return handleCreateDatabase(tokens.get(2));
        } else if (isUseDatabaseStatement(tokens)) {
            return handleUseDatabase(tokens);
        }else if(isCreateTableStatement(tokens)) {
            return handleCreateTable(tokens);
        } else if(isDropDatabaseStatement(tokens)) {
            return handleDropDatabase(tokens);
        } else if(isDropTableStatement(tokens)) {
            return handleDropTable(tokens);
        } else if(isInsertStatement(tokens)) {
            return handleInsert(tokens);
        } else if(isAlterTableStatement(tokens)) {
            return handleAlterTable(tokens);
        } else if(isSelectStatement(tokens)) {
            return handleSelect(tokens);
        } else if(isUpdateStatement(tokens)) {
            return handleUpdate(tokens);
        } else if(isDeleteStatement(tokens)) {
            return handleDelete(tokens);
        } else if(isJoinStatement(tokens) ) {
            return handleJoin(tokens);
        }
        return "[ERROR] Invalid command!";
    }

    private String handleJoin(ArrayList<String> tokens) {
        String tb1 = tokens.get(1);
        String tb2 = tokens.get(3);
        String at1 = tokens.get(5);
        String at2 = tokens.get(7);
        if(!isAlphanumeric(tb1)) return "[ERROR] Table " + tb1 + " is not alphanumeric";
        if(!isAlphanumeric(tb2)) return "[ERROR] Table " + tb2 + " is not alphanumeric";
        if(!isAlphanumeric(at1)) return "[ERROR] Attribute " + at1 + " is not alphanumeric";
        if(!isAlphanumeric(at2)) return "[ERROR] Attribute " + at2 + " is not alphanumeric";
        Database db = dataBaseSupreme.getCurrentDatabase();
        Table table1 = getTableFromName(tb1);
        if(table1 == null) return "[ERROR] Table " + tb1 + " not found";
        if(table1.checkAttribute(at1)) return "[ERROR] Attribute " + at1 + " does not exist";
        Table table2 = getTableFromName(tb2);
        if(table2 == null) return "[ERROR] Table " + tb2 + " not found";
        if(table2.checkAttribute(at2)) return "[ERROR] Attribute " + at2 + " does not exist";
        List<Map<String, String>> tableMap1 = convertDataToListMap(table1.data,table1.getColumns());
        List<Map<String, String>> tableMap2 = convertDataToListMap(table2.data, table2.getColumns());
        List<Map<String, String>> joinedTable = new ArrayList<>();
        for (Map<String, String> row1 : tableMap1) {
            for (Map<String, String> row2 : tableMap2) {
                if (row1.containsKey(at1) && row2.containsKey(at2) &&
                        row1.get(at1).equals(row2.get(at2))) {
                    Map<String, String> joinedRow = new HashMap<>();
                    for (Map.Entry<String, String> entry : row1.entrySet()) {
                        if (!entry.getKey().equals(at1)) {
                            joinedRow.put(tb1 + "." + entry.getKey(), entry.getValue());
                        }
                    }
                    for (Map.Entry<String, String> entry : row2.entrySet()) {
                        if (!entry.getKey().equals(at2)) {
                            joinedRow.put(tb2 + "." + entry.getKey(), entry.getValue());
                        }
                    }
                    joinedTable.add(joinedRow);
                }
            }
        }
        String finalResult = joinedTable.toString();
        return "[OK]" + finalResult;
    }

    private List<Map<String, String>> convertDataToListMap(List<List<String>> data, List<String> columns) {
        List<Map<String, String>> result = new ArrayList<>();
        if (data.isEmpty() || columns.isEmpty()) { return result;}
        for (List<String> row : data) {
            Map<String, String> rowMap = new HashMap<>();
            for (int j = 0; j < columns.size() && j < row.size(); j++) {
                rowMap.put(columns.get(j), row.get(j));
            }
            result.add(rowMap);
        }
        return result;
    }

    private String handleDelete(ArrayList<String> tokens) {
        if (tokens.size() < 4 || !tokens.get(1).equalsIgnoreCase("FROM")) { return "[ERROR]: Invalid DELETE query syntax!";}
        String tableName = tokens.get(2);
        if(!isAlphanumeric(tableName)) return "[ERROR] Table " + tableName + " is not alphanumeric";
        Database db = dataBaseSupreme.getCurrentDatabase();
        if (db == null) { return "[ERROR]: Database not set!";}
        if (!db.searchTable(tableName)) {
            File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
            if (!tableFile.exists()) { return "[ERROR]: Table " + tableName + " does not exist!";}
            db.loadTableFromFile(tableFile); // Load table into memory
        }
        Table table = db.getTable(tableName);
        if (table == null) { return "[ERROR]: Failed to retrieve table!";}
        String whereClause = extractWhereClause(tokens);
        if(whereClause == null) return "[ERROR] where clause shouldn't have non-alphanumeric";
        Condition condition = whereClause.isEmpty() ? null : ConditionParser.parse(whereClause);
        List<Map<String, String>> tableData = table.convertTableDataToMap(table.getData(), table.getColumns());
        List<Map<String, String>> updatedData = new ArrayList<>();
        int deletedCount = 0;
        for (Map<String, String> row : tableData) {
            if (condition == null || !condition.evaluate(row)) {
                updatedData.add(row); // Keep row if it does not satisfy the condition
            } else {
                deletedCount++;
            }
        }
        if (deletedCount == 0) { return "[ERROR]: No matching rows found!";}
        List<List<String>> updatedTableData = table.convertMapToTableData(updatedData);
        table.setData(updatedTableData);
        try {
            updateTableFile(new File(storagePath + "/" + db.getName(), tableName + ".tab"), table);
        } catch (IOException e) {
            return "[ERROR]: Failed to update table file!";
        }
        return "[OK] Deleted " + deletedCount + " rows from '" + tableName + "'.";
    }

    private String handleUpdate(ArrayList<String> tokens) {
        if (tokens.size() < 6 || !tokens.get(2).equalsIgnoreCase("SET")) { return "[ERROR]: Invalid UPDATE query syntax!";}
        String tableName = tokens.get(1);
        if(!isAlphanumeric(tableName)) return "[ERROR] Table " + tableName + " is not alphanumeric";
        Database db = dataBaseSupreme.getCurrentDatabase();
        if(db == null) { return "[ERROR]: Database not set!";}
        if (!db.searchTable(tableName)) {
            File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
            if (!tableFile.exists()) { return "[ERROR]: Table " + tableName + " does not exist!";}
            db.loadTableFromFile(tableFile); // Load table into memory
        }
        Table table = db.getTable(tableName);
        if (table == null) { return "[ERROR]: Failed to retrieve table!";}
        int whereIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("WHERE")) {
                whereIndex = i;
                break;
            }
        }
        if (whereIndex == -1) { return "[ERROR]: Missing WHERE clause!";}
        List<String> setClauseTokens = tokens.subList(3, whereIndex);
        Map<String, String> updates = extractSetClause(setClauseTokens);
        if (updates == null) { return "[ERROR] Invalid SET clause: column names must be alphanumeric or = should be there";}
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String value = entry.getValue();
            if (value.startsWith("'") && value.endsWith("'")) {
                continue;
            }
            if (!isAlphanumeric(value)) {
                return "[ERROR] Invalid value clause";
            }
        }
        String whereClause = extractWhereClause(tokens);
        if(whereClause == null) return "[ERROR]: Invalid WHERE clause!";
        Condition condition = whereClause.isEmpty() ? null : ConditionParser.parse(whereClause);
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
        if (!rowUpdated) { return "[ERROR]: No matching rows found!";}
        List<List<String>> updatedTableData = table.convertMapToTableData(tableData);
        table.setData(updatedTableData);
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
                continue;
            }
            if (i + 2 >= tokens.size() || !tokens.get(i + 1).equals("=")) {return null;}
            String columnName = tokens.get(i);
            if (!isAlphanumeric(columnName)) {return null;}
            updates.put(tokens.get(i), tokens.get(i + 2));
            i += 2;
        }
        return updates;
    }

    private String handleSelect(ArrayList<String> tokens) {
        if(tokens.get(tokens.size()-3).equalsIgnoreCase("FROM")) {
            String tableName = tokens.get(tokens.size()-2);
            if(!isAlphanumeric(tableName)) return "[ERROR] Table " + tableName + " is not alphanumeric";
            if (dataBaseSupreme.getCurrentDatabase() == null) { return "[ERROR] Database not set";}
            Database db = dataBaseSupreme.getCurrentDatabase();
            boolean tableInMemory = db.searchTable(tableName);
            if (!tableInMemory) {
                File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
                if (tableFile.exists()) {
                    try {
                        TableLoadResult result = loadTableFromFile(tableFile, tableName);
                        db.addTable(result.table, result.columns);
                        if (!db.searchTable(tableName)) { return "[ERROR]: Failed to load table " + tableName + " into memory!";}
                    } catch (Exception e) {
                        return "[ERROR] loading table from file: " + e.getMessage();
                    }
                } else {
                    return "[ERROR] Table " + tableName + " not found!";
                }
            }
            Table tb = db.getTable(tableName);
            ArrayList<String> columns = new ArrayList<>();
            getAttributes(tokens, columns);
            return tb.displayColumns(columns) + "[OK]";
        }else if(tokens.size() >= 5) {
            List<Map<String,String>> result;
            result = executeSelect(tokens);
            if(result == null) return "[ERROR] invalid command";
            StringBuilder resultString = new StringBuilder();
            resultString.append("[OK] ");
            resultString.append("\n");
            for (Map<String, String> row : result) {
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    resultString.append(entry.getKey())
                            .append(" ")
                            .append(entry.getValue())
                            .append(" ");
                }
                resultString.append("\n");
            }
            return  resultString.toString();
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
        String tableName = tokens.get(fromIndex + 1);
        if (!isAlphanumeric(tableName)) {
            List<Map<String, String>> errorList = new ArrayList<>();
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "[ERROR]Table " + tableName + " is not alphanumeric");
            errorList.add(errorMap);
            return errorList;
        }
        Table table = getTableFromName(tableName);
        if (table == null) return null;
        List<String> selectedColumns = extractSelectedColumns(tokens);
        if(selectedColumns == null) return null;
        String whereClause = extractWhereClause(tokens);
        if(whereClause == null) return null;
        Condition condition = whereClause.isEmpty() ? null : ConditionParser.parse(whereClause);
        if(condition == null) return null;
        List<Map<String, String>> tableRows = table.convertTableDataToMap(table.getRows(), table.getColumns());
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
        for (int i = 1; i < fromIndex; i++) {
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
        if (whereIndex == -1) return "";
        StringBuilder whereClause = new StringBuilder();
        for (int i = whereIndex + 1; i < tokens.size()-1; i++) {
            String token = tokens.get(i);
            if (i + 1 < tokens.size() && isComparisonOperator(tokens.get(i + 1))) {
                if (!isAlphanumeric(token)) {
                    return null;
                }
            }
            whereClause.append(token).append(" ");
        }
        return whereClause.toString().trim();
    }

    private Table getTableFromName(String tableName) {
        Database db = dataBaseSupreme.getCurrentDatabase();
        if (db == null) return null;
        if (!db.searchTable(tableName)) {
            File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
            if (tableFile.exists()) {
                db.loadTableFromFile(tableFile);
            } else {
                return null;
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
        if (selectIndex == -1) { return;}
        int fromIndex = -1;
        for (int i = selectIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("FROM")) {
                fromIndex = i;
                break;
            }
        }
        if (fromIndex == -1) { return;}
        for (int i = selectIndex + 1; i < fromIndex; i++) {
            if (!tokens.get(i).equals(",")) {
                columns.add(tokens.get(i));
            }
        }
        if (columns.size() == 1 && columns.get(0).equals("*")) {
            System.out.println("SELECT * detected - all columns will be selected");
        }
    }

    private String handleAlterTable(ArrayList<String> tokens) {
        // First we check the corner cases and then proceed.
        String tableName = tokens.get(2);
        if(!isAlphanumeric(tableName)) return "[ERROR] Table " + tableName + " is not alphanumeric";
        if (dataBaseSupreme.getCurrentDatabase() == null) { return "[ERROR] Database not set";}
        Database db = dataBaseSupreme.getCurrentDatabase();
        File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
        if (!tableFile.exists()) { return "[ERROR]: Table " + tableName + " does not exist in the file system!";}
        if (!db.searchTable(tableName)) {
            try {
                TableLoadResult result = loadTableFromFile(tableFile, tableName);
                db.addTable(result.table, result.columns);
                if (!db.searchTable(tableName)) { return "[ERROR]: Failed to load table " + tableName + " into memory!";}
            } catch (Exception e) {
                return "[ERROR]: Failed to load table from file: " + e.getMessage();
            }
        }
        Table tb = db.getTable(tableName);
        String attribute = tokens.get(4);
        if (tokens.get(3).equalsIgnoreCase("ADD")) {
            tb.addColumn(attribute);
            try {
                updateTableFile(tableFile, tb);
            } catch (IOException e) {
                return "[ERROR]: Failed to update table file after adding column: " + e.getMessage();
            }
            return "[OK]Column " + attribute + " added!";
        } else if (tokens.get(3).equalsIgnoreCase("DROP")) {
            tb.deleteColumn(attribute);
            try {
                updateTableFile(tableFile, tb);
            } catch (IOException e) {
                return "[ERROR]: Failed to update table file after dropping column: " + e.getMessage();
            }
            return "[OK] Column " + attribute + " dropped!";
        }
        return "[ERROR] Invalid Alter command!";
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
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new Exception("Table file is empty or corrupted");
            }
            String[] columnNames = headerLine.trim().split("\\s+");
            int startIndex = 0;
            if (columnNames.length > 0 && columnNames[0].trim().equals("id")) {
                startIndex = 1;
            }
            for (int i = startIndex; i < columnNames.length; i++) {
                String columnName = columnNames[i].trim();
                if (!columnName.isEmpty()) {
                    table.addColumn(columnName);
                    columns.add(columnName);
                }
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] values = line.split("\t");
                    ArrayList<String> row = new ArrayList<>();
                    for (int i = startIndex; i < Math.min(values.length, columnNames.length); i++) {
                        row.add(values[i].trim());
                    }
                    table.addValues(tableName,row);
                }
            }
            return new TableLoadResult(table, columns);
        } catch (IOException e) {
            throw new Exception("[ERROR] reading table file: " + e.getMessage(), e);
        }
    }

    private void updateTableFile(File tableFile, Table table) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile))) {
            List<String> columns = table.getColumns();
            writer.write(" ");
            for (String column : columns) {
                writer.write(column + " ");
            }
            writer.newLine();
            List<List<String>> rows = table.getRows();
            for (List<String> row : rows) {
                if (row.size() > 0) {
                    writer.write(row.get(0));  // Write the ID
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
        // First check the corner cases and then proceed
        String tableName = tokens.get(2);
        if(!isAlphanumeric(tableName)) return "[ERROR] Table " + tableName + " is not alphanumeric";
        if (dataBaseSupreme.getCurrentDatabase() == null) { return "[ERROR] Database not set";}
        Database db = dataBaseSupreme.getCurrentDatabase();
        if (db == null) { return "[ERROR] Database " + dataBaseSupreme.getCurrentDatabase().getName() + " does not exist!";}
        if (db.searchTable(tableName)) {
            Table tb = db.getTable(tableName);
            db.deleteTable(tb);
            return "[OK] Table " + tableName + " has been dropped!";
        }
        File dbFolder = new File(storagePath + "/" + db.getName());
        File tableFile = new File(dbFolder, tableName + ".tab");
        if (tableFile.exists() && tableFile.isFile()) {
            boolean deleted = tableFile.delete();
            if (deleted) {
                System.out.println("Table file " + tableFile.getAbsolutePath() + " deleted.");
                return "[OK] Table " + tableName + " has been dropped from the file system!";
            } else {
                return "[ERROR] Failed to delete table file " + tableFile.getAbsolutePath();
            }
        }
        return "[ERROR] Table " + tableName + " does not exist!";
    }

    private String handleDropDatabase(ArrayList<String> tokens) {
        String toDeleteDatabase = tokens.get(2);
        if(!isAlphanumeric(toDeleteDatabase)) return "[ERROR] Database " + toDeleteDatabase + " is not alphanumeric";
        Database db = dataBaseSupreme.getDatabase(toDeleteDatabase);
        if(db == null) {
            return "[ERROR] Database does not exist!";
        }
        File dbFolder = new File(storagePath, toDeleteDatabase);
        if (dbFolder.exists() && dbFolder.isDirectory()) {
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
        return "[OK] Database dropped!";
    }

    private void deleteDirectory(File folder) throws IOException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file); // Recursive function ehhhh
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
        if (db == null) { return "[ERROR] No database is in use";}
        String tableName = tokens.get(2);
        if(!isAlphanumeric(tableName)) return "[ERROR] Table " + tableName + " is not alphanumeric";
        if (!db.searchTable(tableName)) {
            File tableFile = new File(storagePath, db.getName() + "/" + tableName + ".tab");
            if (!tableFile.exists()) {
                return "[ERROR] Table " + tableName + " does not exist in the database.";
            } else {
                db.loadTableFromFile(tableFile);
            }
        }
        Table tb = db.getTable(tableName);
        tb.databaseName = db.getName();
        if (tb == null) { return "[ERROR]: Unable to retrieve table " + tableName;}
        ArrayList<String> values = new ArrayList<>();
        getValues(tokens, values);
        if(values.size() + 1 != tb.colNames.size() ) { return "[ERROR] Table has" + (tb.totalColumns - 1) + " column(s)!";}
        tb.addValues(tableName, values);
        return "[OK] Inserted into " + tableName + " values " + values;
    }

    private void getValues(ArrayList<String> tokens, ArrayList<String> values) {
        int valuesIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equalsIgnoreCase("VALUES")) {
                valuesIndex = i;
                break;
            }
        }
        // If "VALUES" keyword not found ??
        if (valuesIndex == -1) {
            return;
        }
        // get the opening parenthesis that comes after values
        int openParenIndex = -1;
        for (int i = valuesIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).equals("(")) {
                openParenIndex = i;
                break;
            }
        }
        // If no opening parenthesis  ???
        if (openParenIndex == -1) {
            return;
        }
        // After open parenthesis
        int i = openParenIndex + 1;
        // Until closing parenthesis
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
        if (db == null) { return "[ERROR]: No database is in use";}
        String tableName = tokens.get(2);
        if(!isAlphanumeric(tableName)) return "[ERROR] Table " + tableName + " is not alphanumeric";
        if (db.searchTable(tableName)) { return "[ERROR]: Table already exists!";}
        File tableFile = new File(storagePath + "/" + db.getName(), tableName + ".tab");
        if (tableFile.exists()) { return "[ERROR]: Table file already exists in the file system!";}
        ArrayList<String> columns = new ArrayList<>();
        getColumns(tokens, columns);
        for(String s : columns)
        {
            if(!isAlphanumeric(s)) return "[ERROR] Column " + s + " is not alphanumeric";
        }
        Table tb = new Table(tableName,columns, db.getName());
        db.addTable(tb, columns);
        try {
                tableFile.createNewFile();
                return "[OK] Table '" + tableName + "' created successfully!";
        } catch (IOException e) {
            return "[ERROR]: IOException occurred while creating the table file: " + e.getMessage();
        }

    }

    private void getColumns(ArrayList<String> tokens, ArrayList<String> columns)
    {
        int openParenIndex = tokens.indexOf("(");
        if(openParenIndex == -1) return;
        // After open parenthesis
        int i = openParenIndex + 1;
        // Until closing parenthesis
        while (!tokens.get(i).equals(")")) {
            // Skip commas
            if (!tokens.get(i).equals(",")) {
                columns.add(tokens.get(i));
            }
            i++;
        }
    }

    private String handleCreateDatabase(String dbName) {
        if(!isAlphanumeric(dbName)) return "[ERROR] Database: " + dbName + " is not alphanumeric";
        // In our memory if database exists
        if (dataBaseSupreme.searchDatabases(dbName)) {
            return "[ERROR]: Database already exists in memory.";
        }
        // In our file system if database exists
        File dbFolder = new File(storagePath, dbName);
        if (dbFolder.exists() && dbFolder.isDirectory()) {
            return "[ERROR]: Database folder already exists in the file system.";
        }
        // New folder for Database folder
        if (dbFolder.mkdirs()) {
            dataBaseSupreme.addDatabase(new Database(dbName, storagePath));
            return "[OK] Database '" + dbName + "' created successfully!";
        } else {
            return "[ERROR]: Failed to create database.";
        }
    }

    private String handleUseDatabase(ArrayList<String> tokens) {
        String dbName = tokens.get(1);
        if(!isAlphanumeric(dbName)) return "[ERROR] Database: " + dbName + " is not alphanumeric";
        if (dataBaseSupreme.currentDatabase != null && dataBaseSupreme.currentDatabase.equals(dbName)) {
            return "[OK] Already in the same database";
        }
        // In memory ???
        if (dataBaseSupreme.searchDatabases(dbName)) {
            dataBaseSupreme.setCurrentDatabase(dbName);
            return "[OK] Current database set to " + dbName;
        }
        // Database exists or not mate ???
        File dbFolder = new File(storagePath, dbName);
        if (dbFolder.exists() && dbFolder.isDirectory()) {
            dataBaseSupreme.addDatabase(new Database(dbName, storagePath));
            dataBaseSupreme.setCurrentDatabase(dbName);
            return "[OK] Current database set to " + dbName;
        }
        return "[ERROR]: Database not found in memory or file system.";
    }

    public boolean isAlphanumeric(String str) {
        if (str == null || str.isEmpty()) { return false;}
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isLetterOrDigit(c)) { return false;}
        }
        return true;
    }

    private boolean isJoinStatement(List<String> tokens) {
        if (tokens.size() != 9) return false;
        return tokens.get(0).equalsIgnoreCase("JOIN") &&
                tokens.get(2).equalsIgnoreCase("AND") &&
                tokens.get(4).equalsIgnoreCase("ON") &&
                tokens.get(6).equalsIgnoreCase("AND") &&
                tokens.get(8).equalsIgnoreCase(";");
    }

    private boolean isDeleteStatement(List<String> tokens) {
        return tokens.size() >= 3 &&
                tokens.get(0).equalsIgnoreCase("DELETE") &&
                tokens.get(1).equalsIgnoreCase("FROM") &&
                tokens.get(tokens.size()-1).equals(";");
    }

    private boolean isUpdateStatement(List<String> tokens) {
        return tokens.size() >= 4 &&
                tokens.get(0).equalsIgnoreCase("UPDATE") &&
                tokens.get(2).equalsIgnoreCase("SET") &&
                tokens.get(tokens.size()-1).equals(";");
    }

    private boolean isSelectStatement(List<String> tokens) {
        return tokens.size() >= 5 &&
                tokens.get(0).equalsIgnoreCase("SELECT") &&
                tokens.get(tokens.size()-1).equals(";");
    }

    private boolean isAlterTableStatement(List<String> tokens) {
        return tokens.size() == 6 &&
                tokens.get(0).equalsIgnoreCase("ALTER") &&
                tokens.get(1).equalsIgnoreCase("TABLE") &&
                tokens.get(tokens.size()-1).equals(";");
    }

    private boolean isInsertStatement(List<String> tokens) {
        return tokens.size() >= 6 &&
                tokens.get(0).equalsIgnoreCase("INSERT") &&
                tokens.get(1).equalsIgnoreCase("INTO") &&
                tokens.get(3).equalsIgnoreCase("VALUES") &&
                tokens.get(4).equals("(") &&
                tokens.get(tokens.size()-2).equals(")") &&
                tokens.get(tokens.size()-1).equals(";");
    }

    private boolean isDropTableStatement(List<String> tokens) {
        return tokens.size() == 4 &&
                tokens.get(0).equalsIgnoreCase("DROP") &&
                tokens.get(1).equalsIgnoreCase("TABLE") &&
                tokens.get(tokens.size()-1).equals(";");
    }

    private boolean isDropDatabaseStatement(List<String> tokens) {
        return tokens.size() == 4 &&
                tokens.get(0).equalsIgnoreCase("DROP") &&
                tokens.get(1).equalsIgnoreCase("DATABASE") &&
                tokens.get(tokens.size()-1).equals(";");
    }

    private boolean isCreateTableStatement(List<String> tokens) {
        boolean isSimpleFormat = tokens.size() >= 3 &&
                tokens.get(0).equalsIgnoreCase("CREATE") &&
                tokens.get(1).equalsIgnoreCase("TABLE") &&
                tokens.get(tokens.size()-1).equals(";");
        boolean isDetailedFormat = tokens.size() >= 7 &&
                tokens.get(1).equalsIgnoreCase("CREATE") &&
                tokens.get(2).equalsIgnoreCase("TABLE") &&
                tokens.get(3).equals("(") &&
                tokens.get(tokens.size()-2).equals(")") &&
                tokens.get(tokens.size()-1).equals(";");
        return isSimpleFormat || isDetailedFormat;
    }

    private boolean isUseDatabaseStatement(List<String> tokens) {
        return tokens.size() >= 2 &&
                tokens.get(0).equalsIgnoreCase("USE") &&
                tokens.get(tokens.size() - 1).equals(";");
    }

    private boolean isCreateDatabaseStatement(List<String> tokens) {
        return tokens.size() >= 3 &&
                tokens.get(0).equalsIgnoreCase("CREATE") &&
                tokens.get(1).equalsIgnoreCase("DATABASE") &&
                tokens.get(tokens.size() - 1).equals(";");
    }

    private boolean isComparisonOperator(String token) {
        return token.equals("=") || token.equals("==") || token.equals("!=") ||
                token.equals("<") || token.equals(">") || token.equals("<=") ||
                token.equals(">=");
    }

    public void setCurrentDatabase(String currentDatabase) {
        this.currentDatabase = currentDatabase;
    }
}
