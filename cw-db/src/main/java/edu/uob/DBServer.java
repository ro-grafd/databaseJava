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

        if (command.startsWith("CREATE DATABASE")) {
            return dbManager.createDatabase(command.split(" ")[2]);
        } else if (command.startsWith("USE")) {
            return useDatabase(command.split(" ")[1]);
        } else if (command.startsWith("CREATE TABLE")) {
            return tableManager.createTable(command.split(" ")[2]);
        }
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
