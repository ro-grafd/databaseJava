package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.*;

class Database {
    private String name;
    private List<Table> tables;
    private String storagePath;

    public Database(String name, String storagePath) {
        this.storagePath = storagePath;
        this.name = name;
        this.tables = new ArrayList<>();
    }

    public void deleteTable(Table tableToDelete) {
        tables.remove(tableToDelete);
        File dbFolder = new File(storagePath + "/" + name);
        File tableFile = new File(dbFolder, tableToDelete.getName() + ".tab");
        if (tableFile.exists() && tableFile.isFile()) {
            boolean deleted = tableFile.delete();
            if (deleted) {
                System.out.println("Table file deleted: " + tableFile.getAbsolutePath());
            } else {
                System.out.println("Failed to delete table file: " + tableFile.getAbsolutePath());
            }
        } else {
            System.out.println("Warning: Table file not found at: " + tableFile.getAbsolutePath());
        }
    }

    public void addTable(Table table, ArrayList<String> columns) {
        tables.add(table);
        File dbFolder = new File(storagePath + "/" + name);
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        }
        System.out.println("Database folder path: " + dbFolder.getAbsolutePath());
        File tableFile = new File(dbFolder, table.getName() + ".tab");  // Use .tab or any extension
        System.out.println("Table file exists: " + tableFile.exists());
        System.out.println("Table file path: " + tableFile.getAbsolutePath());
        if (!tableFile.exists()) {
            try {
                boolean fileCreated = tableFile.createNewFile();
                if (fileCreated) {
                    System.out.println("Table file created: " + tableFile.getAbsolutePath());
                    try (FileWriter writer = new FileWriter(tableFile)) {
                        writer.write("id");
                        if (columns != null && !columns.isEmpty()) {
                            for (String column : columns) {
                                writer.write( "\t" + column);
                            }
                        }
                        writer.write("\n");
                        System.out.println("Column headers added to the table file.");
                    } catch (IOException e) {
                        System.out.println("Error writing to table file: " + e.getMessage());
                    }
                } else {
                    System.out.println("Failed to create the table file.");
                }
            } catch (IOException e) {
                System.out.println("Error creating table file: " + e.getMessage());
            }
        }
    }

    public String getName()
    {
        return name;
    }

    public boolean searchTable(String tableName) {
        for (Table tb : tables) {
            if(tb.getName().equals(tableName))
            {
                return true;
            }
        }
        return false;
    }

    public Table getTable(String tableName) {
        for (Table tb : tables) {
            if(tb.getName().equals(tableName)){
                return tb;
            }
        }
        return null;
    }

    public void displayDatabaseDetails() {
        System.out.println("Database: " + name);
        for (Table table : tables) {
            table.displayTableDetails();
            System.out.println("------------------");
        }
    }

    public void loadTableFromFile(File tableFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            String headerLine = reader.readLine(); // Read first line (column headers)
            if (headerLine == null || headerLine.trim().isEmpty()) {
                System.out.println("ERROR: Table file is empty or corrupted: " + tableFile.getName());
                return;
            }
            ArrayList<String> columns = new ArrayList<>(Arrays.asList(headerLine.trim().split("\\s+")));
            if (columns.get(0).equals("id")) {
                columns.remove(0);
            }
            Table table = new Table(tableFile.getName().replace(".tab", ""), columns, this.name);
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> rowData = new ArrayList<>(Arrays.asList(line.split("\\s*,\\s*")));
                table.data.add(rowData);
            }
            table.totalRows = table.data.size();
            table.totalColumns = table.colNames.size();
            tables.add(table);
            System.out.println("Table '" + table.getName() + "' loaded successfully from file.");
        } catch (IOException e) {
            System.out.println("ERROR: Unable to read table file: " + tableFile.getName());
        }
    }

    public boolean searchTableFile(String tableName) {
        return tables.stream().anyMatch(t -> t.getName().equalsIgnoreCase(tableName));
    }

    public Table getTableFile(String tableName) {
        return tables.stream()
                .filter(t -> t.getName().equalsIgnoreCase(tableName))
                .findFirst()
                .orElse(null);
    }
}