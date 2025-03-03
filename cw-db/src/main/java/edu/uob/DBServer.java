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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    private DatabaseManager dbManager;
    private TableManager tableManager;
    private String currentDatabase;

    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8000);
    }

    /**
    * KEEP this signature otherwise we won't be able to mark your submission correctly.
    */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        dbManager = new DatabaseManager(storageFolderPath);
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */
    public String handleCommand(String command) {
        // TODO implement your server logic here
        command = command.trim().toUpperCase();
        CreateTokenizer tokenizer = new CreateTokenizer(); // Later without instance also it will, ask Simon!!!
        String[] words = tokenizer.tokenizeQuery(command);
        if (Objects.equals(words[0], "CREATE") && Objects.equals(words[1], "DATABASE") && Objects.equals(words[words.length-1], ";")) {
            return dbManager.createDatabase(words[2]);
        } else if (Objects.equals(words[0], "USE") && Objects.equals(words[words.length-1], ";")) {
//            for (String word : words) {
//                System.out.println(word);
//            }
            return useDatabase(words[1]);
        } else if (Objects.equals(words[0], "CREATE") && Objects.equals(words[1], "TABLE") && Objects.equals(words[words.length-1], ";")) {
            // Extract table name from the tokenized query
            String tableName = words[2]; // Third word is the table name

            // Extract attributes from the words array
            ArrayList<String> attributes = new ArrayList<>();
            boolean insideParentheses = false;

            for (int i = 3; i < words.length; i++) {
                if (words[i].equals("(")) {
                    insideParentheses = true;
                    continue;
                }
                if (words[i].equals(")")) {
                    insideParentheses = false;
                    break;
                }
                if (insideParentheses && !Objects.equals(words[i], ",")) {
                    attributes.add(words[i]);
                }
            }

            // Print results (for debugging)
            System.out.println("Table Name: " + tableName);
            System.out.println("Attributes: " + attributes);
//            for (String s : attributes) {
//                System.out.println(s);
//            }
            return tableManager.createTable(words[2], attributes);
        }
//

//        else if (command.startsWith("INSERT INTO")) {
//            String[] parts = command.split(" ", 4);
//            return tableManager.insertIntoTable(parts[2], parts[3]);
//        } else if (command.startsWith("SELECT * FROM")) {
//            return tableManager.selectFromTable(command.split(" ")[3]);
//        }
        else {
            return "ERROR: Invalid SQL command.";
        }
    }

    private String useDatabase(String dbName) {
        if (!dbManager.databaseExists(dbName)) {
            return "ERROR: Database does not exist.";
        }
        currentDatabase = dbName;
        tableManager = new TableManager(storageFolderPath + "/" + currentDatabase);
        return "Using database '" + dbName + "'.";
    }

    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
