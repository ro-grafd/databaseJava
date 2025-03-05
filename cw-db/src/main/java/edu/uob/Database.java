package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

    public void addTable(Table table) {
        tables.add(table);
        // Ensure the database folder exists
        File dbFolder = new File(storagePath + "/" + name);  // Assuming the database folder is named after the database
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();  // Create the directory if it doesn't exist
        }

        // Debug: Check the folder path
        System.out.println("Database folder path: " + dbFolder.getAbsolutePath());

        // Create a file for the table (table name will be used as the filename)
        File tableFile = new File(dbFolder, table.getName() + ".tab");  // Use .tab or any extension

        // Debug: Check if file already exists
        System.out.println("Table file exists: " + tableFile.exists());
        System.out.println("Table file path: " + tableFile.getAbsolutePath());

        // If the table file doesn't exist, create it
        if (!tableFile.exists()) {
            try {
                boolean fileCreated = tableFile.createNewFile();
                if (fileCreated) {
                    System.out.println("Table file created: " + tableFile.getAbsolutePath());
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
    public void displayDatabaseDetails() {
        System.out.println("Database: " + name);
        for (Table table : tables) {
            table.displayTableDetails();
            System.out.println("------------------");
        }
    }
}