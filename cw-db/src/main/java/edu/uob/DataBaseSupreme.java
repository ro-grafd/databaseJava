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

    public Database getDatabase(String databaseDelete) {
        for (Database database : databases) {
            if (database.getName().equals(databaseDelete)) {
                return database;
            }
        }
        File dbFolder = new File(storagePath, databaseDelete);
        if (dbFolder.exists() && dbFolder.isDirectory()) {
            Database db = new Database(databaseDelete, storagePath);
            loadTablesFromFileSystem(db);
            databases.add(db);  // Add the database to the in-memory list
            return db;
        }
        return null;
    }

    private void loadTablesFromFileSystem(Database db) {
        File dbFolder = new File(storagePath, db.getName());
        File[] tableFiles = dbFolder.listFiles((dir, name) -> name.endsWith(".tab"));
        if (tableFiles != null) {
            for (File tableFile : tableFiles) {
                db.loadTableFromFile(tableFile);
            }
        }
    }

    public void deleteDatabase(Database databaseToDelete) {
        databases.remove(databaseToDelete);
        databasenames.remove(databaseToDelete.getName());
        Path dbDirectoryPath = Paths.get(storagePath, databaseToDelete.getName());
        File dbDirectory = dbDirectoryPath.toFile();
        if (dbDirectory.exists() && dbDirectory.isDirectory()) {
            try {
                deleteDirectoryRecursively(dbDirectory);
                System.out.println("Database directory deleted: " + dbDirectoryPath.toAbsolutePath());
            } catch (IOException e) {
                System.out.println("Error deleting database directory: " + e.getMessage());
            }
        } else {
            System.out.println("Warning: Database directory not found at: " + dbDirectoryPath.toAbsolutePath());
        }
    }

    // Helper method to recursively delete a directory
    private void deleteDirectoryRecursively(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException("Failed to delete file: " + file);
                    }
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete directory: " + directory);
        }
    }

    public void addDatabase(Database db) {
        Path dbDirectoryPath = Paths.get(storagePath, db.getName());
        try {
            Files.createDirectories(dbDirectoryPath);
            System.out.println("Database directory created at: " + dbDirectoryPath.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error creating database directory: " + e.getMessage());
            return;
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