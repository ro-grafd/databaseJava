package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;

public class MyTests {

    private DBServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    // Random name generator - useful for testing "bare earth" queries (i.e. where tables don't previously exist)
    private String generateRandomName() {
        String randomName = "";
        for(int i=0; i<10 ;i++) randomName += (char)( 97 + (Math.random() * 25.0));
        return randomName;
    }

    private String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // My test cases
    @Test
    public void testForUpdate(){
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE tb1 (at1, at2, at3);");
        sendCommandToServer("INSERT INTO tb1 VALUES (v4, v8, v6);");
        sendCommandToServer("INSERT INTO tb1 VALUES (v7, v8, v6);");
        sendCommandToServer("UPDATE tb1 SET at2 = BB WHERE at3 == v6;");
        String response = sendCommandToServer("SELECT * FROM tb1;");
        assertTrue(response.contains("BB"), "To find if we have updated the table, however it was not returned by SELECT *");
        assertFalse(response.contains("v8"),"To find if the old value is not there, however it was returned by SELECT *");
        sendCommandToServer("DROP DATABASE " + randomName + ";");
    }

    @Test
    public void testForSelect(){
        server = new DBServer();
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE tb2 (at1, at2, at3);");
        sendCommandToServer("INSERT INTO tb2 VALUES (v1, v2, v3);");
        sendCommandToServer("INSERT INTO tb2 VALUES (v4, v5, v6);");
        String response = sendCommandToServer("SELECT at3 FROM tb2 WHERE at1==v1 AND at2 == v2;");
        assertTrue(response.contains("v3"), "To find if compound condition handles, however it was not returned by SELECT ");
        assertFalse(response.contains("v2"), "To find if compound condition handles, however it WAS returned");
        sendCommandToServer("DROP DATABASE " + randomName + ";");
    }
    @Test
    public void testForCreate() {
        server = new DBServer();
        String randomName = generateRandomName();
        String createDbResponse = sendCommandToServer("CREATE DATABASE " + randomName + ";");
        assertTrue(createDbResponse.contains("[OK]"), "Database creation should return OK response");

        String useDbResponse = sendCommandToServer("USE " + randomName + ";");
        assertTrue(useDbResponse.contains("[OK]"), "Using the database should return OK response");

        String createTableResponse = sendCommandToServer("CREATE TABLE testTable (column1, column2, column3);");
        assertTrue(createTableResponse.contains("[OK]"), "Table creation should return OK response");

        sendCommandToServer("INSERT INTO testTable VALUES (val1, val2, val3);");
        String selectResponse = sendCommandToServer("SELECT * FROM testTable;");
        assertTrue(selectResponse.contains("val1"), "Should return inserted values");
        assertTrue(selectResponse.contains("val2"), "Should return inserted values");
        assertTrue(selectResponse.contains("val3"), "Should return inserted values");

        String duplicateTableResponse = sendCommandToServer("CREATE TABLE testTable (column1, column2);");
        assertTrue(duplicateTableResponse.contains("[ERROR]"), "Creating duplicate table should return ERROR");

        String invalidTableResponse = sendCommandToServer("CREATE TABLE 1@3invalid (column1);");
        assertTrue(invalidTableResponse.contains("[ERROR]"), "Creating table with invalid name should return ERROR");

        String invalidColumnResponse = sendCommandToServer("CREATE TABLE validTable (column@1);");
        assertTrue(invalidColumnResponse.contains("[ERROR]"), "Creating table with invalid column name should return ERROR");
        sendCommandToServer("DROP DATABASE " + randomName + ";");
    }
    @Test
    public void testForAlter() {
        server = new DBServer();
        String randomName = generateRandomName();

        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE testTable (column1, column2);");
        sendCommandToServer("INSERT INTO testTable VALUES (val1, val2);");

        String alterAddResponse = sendCommandToServer("ALTER TABLE testTable ADD column3;");
        assertTrue(alterAddResponse.contains("[OK]"), "Altering table to add column should return OK response");

        sendCommandToServer("INSERT INTO testTable VALUES (newVal1, newVal2, newVal3);");
        String selectResponse = sendCommandToServer("SELECT * FROM testTable;");
        assertTrue(selectResponse.contains("newVal3"), "Should contain value inserted into new column");

        assertTrue(selectResponse.contains("val1") && selectResponse.contains("val2"),
                "Old data should be preserved after altering table");

        String alterDropResponse = sendCommandToServer("ALTER TABLE testTable DROP column2;");
        assertTrue(alterDropResponse.contains("[OK]"), "Altering table to drop column should return OK response");

        String selectAfterDropResponse = sendCommandToServer("SELECT * FROM testTable;");
        assertTrue(selectAfterDropResponse.contains("column1") &&
                        selectAfterDropResponse.contains("column3"),
                "Should keep remaining columns");
        assertFalse(selectAfterDropResponse.contains("column2"),
                "Dropped column should not appear in results");
        assertTrue(selectAfterDropResponse.contains("val1"), "Data for remaining columns should be preserved");
        assertTrue(selectAfterDropResponse.contains("newVal1") &&
                        selectAfterDropResponse.contains("newVal3"),
                "Data for remaining columns should be preserved");

        String nonExistentTableResponse = sendCommandToServer("ALTER TABLE nonExistentTable ADD newColumn;");
        assertTrue(nonExistentTableResponse.contains("[ERROR]"),
                "Altering non-existent table should return ERROR");

        sendCommandToServer("DROP DATABASE " + randomName + ";");
    }
}
