package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.FileWriter;
import java.util.List;

public class TableManager {
    private String databasePath;

    public TableManager(String dbPath) {
        this.databasePath = dbPath;
    }

    public String createTable(String tableName , List<String> attributes) {
        File tableFile = new File(databasePath, tableName + ".csv");

        if (tableFile.exists()) {
            return "ERROR: Table already exists.";
        }

        try {
            if (tableFile.createNewFile()) {
                // Write attributes to the file as the first line
                try (FileWriter writer = new FileWriter(tableFile)) {
                    writer.write(String.join(",", attributes) + "\n");
                }
                return "Table '" + tableName + "' created successfully with attributes: " + attributes;
            } else {
                return "ERROR: Failed to create table.";
            }
        } catch (IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }

//    public String insertIntoTable(String tableName, String values) {
//        File tableFile = new File(databasePath, tableName + ".txt");
//        if (!tableFile.exists()) {
//            return "ERROR: Table does not exist.";
//        }
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true))) {
//            writer.write(values + "\n");
//            return "Row inserted into '" + tableName + "'.";
//        } catch (IOException e) {
//            return "ERROR: " + e.getMessage();
//        }
//    }

//    public String selectFromTable(String tableName) {
//        File tableFile = new File(databasePath, tableName + ".txt");
//        if (!tableFile.exists()) {
//            return "ERROR: Table does not exist.";
//        }
//        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
//            StringBuilder result = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                result.append(line).append("\n");
//            }
//            return result.toString();
//        } catch (IOException e) {
//            return "ERROR: " + e.getMessage();
//        }
//    }
}
