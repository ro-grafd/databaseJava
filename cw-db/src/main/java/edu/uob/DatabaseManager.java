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

public class DatabaseManager {
    private String storageFolderPath;

    public DatabaseManager(String storagePath) {
        this.storageFolderPath = storagePath;
    }

    public String createDatabase(String dbName) {
        File dbFolder = new File(storageFolderPath, dbName);
        if (dbFolder.exists()) {
            return "ERROR: Database already exists.";
        }
        if (dbFolder.mkdirs()) {
            return "Database '" + dbName + "' created successfully.";
        } else {
            return "ERROR: Failed to create database.";
        }
    }

    public boolean databaseExists(String dbName) {
        return new File(storageFolderPath, dbName).exists();
    }
}
