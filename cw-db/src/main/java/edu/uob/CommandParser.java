package edu.uob;

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
        if (tokens.size() >= 3 && tokens.get(0).equals("CREATE") && tokens.get(1).equals("DATABASE")) {
            // Handle CREATE DATABASE command
            return handleCreateDatabase(tokens.get(2));
        }
        else if (tokens.size() >= 2 && tokens.get(0).equals("USE")) {
            return handleUseDatabase(tokens);
        }else if(tokens.size() >=3 && tokens.get(0).equals("CREATE") && tokens.get(1).equals("TABLE")) {
            return handleCreateTable(tokens);
        }
        // Handle other types of commands (if any) here
        return "Invalid command!";
    }
    private String handleCreateTable(ArrayList<String> tokens)
    {
        Database db = dataBaseSupreme.getCurrentDatabase();
        if(db == null)
        {
            return "No database is in use";
        }
        String tableName = tokens.get(2);
        if(db.searchTable(tableName))
        {
            return "Table already exists!";
        }




        db.addTable(new Table(tableName));
        return "Table created!";
    }
    private String handleCreateDatabase(String dbName) {
        // Check if the database already exists
        if (dataBaseSupreme.searchDatabases(dbName)) {
            return "Database already exists";
        }

        // Create the new database and add it to the list
        dataBaseSupreme.addDatabase(new Database(dbName, storagePath));
        return "Database " + dbName + " created successfully!";
    }
private String handleUseDatabase(ArrayList<String> tokens) {
    if (dataBaseSupreme.currentDatabase != null && dataBaseSupreme.currentDatabase.equals(tokens.get(1))) {
        return "Already in the same database";
    } else {
        // Check if the database exists
        if (dataBaseSupreme.searchDatabases(tokens.get(1))) {
            dataBaseSupreme.setCurrentDatabase(tokens.get(1));
            return "Current database set to " + tokens.get(1);
        } else {
            return "Database not found";
        }
    }
}
}
