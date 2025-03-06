package edu.uob;

import javax.xml.crypto.Data;
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
        }
        // Handle other types of commands (if any) here
        return "Invalid command!";
    }
    private String handleDropTable(ArrayList<String> tokens) {
        String tableName = tokens.get(2);
        Database db = dataBaseSupreme.getCurrentDatabase();
        if(!db.searchTable(tableName))
        {
            return "Table " + tableName + " does not exist!";
        }
        Table tb = db.getTable(tableName);
        db.deleteTable(tb);
        return "Table has been dropped!";
    }
    private String handleDropDatabase(ArrayList<String> tokens) {
        String toDeleteDatabase = tokens.get(2);
        Database db = dataBaseSupreme.getDatabase(toDeleteDatabase);
        if(db == null) {
            return "Database does not exist!";
        }
        dataBaseSupreme.deleteDatabase(db);
        return "Database dropped!";
    }
    private String handleInsert(ArrayList<String> tokens) {
        Database db = dataBaseSupreme.getCurrentDatabase();
        if(db == null) {
            return "No database is in use";
        }
        String tableName = tokens.get(2);
        if(!db.searchTable(tableName)){
            return "Table " + tableName + " does not exist";
        }
        Table tb = db.getTable(tableName);
        ArrayList<String> values = new ArrayList<>();
        getValues(tokens, values);
        tb.addValues(tableName, values);
        return "Insert into " + tableName + " values " + values;
    }
    private void getValues(ArrayList<String> tokens, ArrayList<String> values) {
        int valuesIndex = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals("VALUES")) {
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
    private String handleCreateTable(ArrayList<String> tokens)
    {
        Database db = dataBaseSupreme.getCurrentDatabase();
        if(db == null) {
            return "No database is in use";
        }
        String tableName = tokens.get(2);
        if(db.searchTable(tableName)) {
            return "Table already exists!";
        }
        ArrayList<String> columns = new ArrayList<>();
        getColumns(tokens,columns);
        db.addTable(new Table(tableName), columns);
        return "Table created!";
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
