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

class DataBaseSupreme {
    List<Database> databases;
    Database currentDatabase;
    List<String> databasenames;
    String storagePath;
    public DataBaseSupreme(String storageFolderPath) {
        this.storagePath = storageFolderPath;
        this.databases = new ArrayList<>();
        this.databasenames = new ArrayList<>();
    }

    public void addDatabase(Database db) {
        Path dbDirectoryPath = Paths.get(storagePath, db.getName());

        // Try to create the directory
        try {
            Files.createDirectories(dbDirectoryPath); // This will create the directory if it doesn't exist
            System.out.println("Database directory created at: " + dbDirectoryPath.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error creating database directory: " + e.getMessage());
            return;  // Exit the method if directory creation fails
        }
        databases.add(db);
        databasenames.add(db.getName());
    }
    public void addDatabaseName(String dbname) {
        databasenames.add(dbname);
    }
    public void removeDatabase(Database db) {
        databases.remove(db);
    }
    public void removeDatabaseName(String dbname) {
        databasenames.remove(dbname);
    }
    public boolean searchDatabases(String dbName) {
        for(Database db : databases) {
            if(db.getName().equals(dbName)) {
                return true;
            }
        }
        return false;
    }
    public Database getCurrentDatabase() {
        return currentDatabase;
    }
    public void setCurrentDatabase(String dbName) {
        for (Database db : databases) {
            if (db.getName().equals(dbName)) {
                currentDatabase = db;
                System.out.println("Current database set to: " + dbName);
                return;
            }
        }
        System.out.println("Database not found: " + dbName);
    }

    public void displayAllDatabases() {
        for (Database db : databases) {
            db.displayDatabaseDetails();
        }
    }
}