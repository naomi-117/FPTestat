package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private Connection testConnection;

    @BeforeEach
    void setupDatabase() throws Exception {
        testConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/FBTestat", "postgres", "1234");

        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("CREATE TABLE Benutzer (Benutzer_ID INT AUTO_INCREMENT PRIMARY KEY, Benutzername VARCHAR(50), Konto_guthaben DOUBLE);");
            stmt.execute("CREATE TABLE Transaktionen (Transaktion_ID INT AUTO_INCREMENT PRIMARY KEY, Betrag DOUBLE, Beschreibung TEXT, Sender INT, Empfaenger INT, Datum TIMESTAMP);");

            stmt.execute("INSERT INTO Benutzer (Benutzername, Konto_guthaben) VALUES ('SenderUser', 1000.0);");
            stmt.execute("INSERT INTO Benutzer (Benutzername, Konto_guthaben) VALUES ('ReceiverUser', 500.0);");
        }
    }

    @AfterEach
    void teardownDatabase() throws Exception {
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("DROP TABLE Transaktionen;");
            stmt.execute("DROP TABLE Benutzer;");
        }
        testConnection.close();
    }

    @Test
    void singleTransaction() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();
        double amount = 200.0;
        String description = "Test Transaction";

        // Act
        transaction.singleTransaction(amount, description, "SenderUser", "ReceiverUser");

        // Assert
        try (Statement stmt = testConnection.createStatement()) {
            var rs = stmt.executeQuery("SELECT * FROM Transaktionen WHERE Betrag = 200.0;");
            assertTrue(rs.next());
            assertEquals(description, rs.getString("Beschreibung"));

            rs = stmt.executeQuery("SELECT Konto_guthaben FROM Benutzer WHERE Benutzername = 'SenderUser';");
            assertTrue(rs.next());
            assertEquals(800.0, rs.getDouble("Konto_guthaben"));

            rs = stmt.executeQuery("SELECT Konto_guthaben FROM Benutzer WHERE Benutzername = 'ReceiverUser';");
            assertTrue(rs.next());
            assertEquals(700.0, rs.getDouble("Konto_guthaben"));
        }
    }

    @Test
    void payOutMoney() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();
        double amount = 300.0;

        // Act
        transaction.payOutMoney(amount, "SenderUser");

        // Assert
        try (Statement stmt = testConnection.createStatement()) {
            var rs = stmt.executeQuery("SELECT * FROM Transaktionen WHERE Betrag = 300.0;");
            assertTrue(rs.next());

            rs = stmt.executeQuery("SELECT Konto_guthaben FROM Benutzer WHERE Benutzername = 'SenderUser';");
            assertTrue(rs.next());
            assertEquals(700.0, rs.getDouble("Konto_guthaben"));
        }
    }

    @Test
    void uploadCSVForBulkTransfer() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();
        String csvFilePath = "test.csv";

        // Erstellen einer Test-CSV-Datei
        try (java.io.FileWriter writer = new java.io.FileWriter(csvFilePath)) {
            writer.write("ReceiverUser;100;Description 1\n");
            writer.write("ReceiverUser;200;Description 2\n");
        }

        // Act
        transaction.uploadCSVForBulkTransfer(csvFilePath);

        // Assert
        try (Statement stmt = testConnection.createStatement()) {
            var rs = stmt.executeQuery("SELECT * FROM Transaktionen;");
            assertTrue(rs.next());
            assertTrue(rs.next());
        }

        // Cleanup
        new java.io.File(csvFilePath).delete();
    }

    @Test
    void exportTransactions() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();
        String filePath = "exported_transactions.txt";

        // Act
        transaction.exportTransactions("SenderUser", filePath);

        // Assert
        java.io.File file = new java.io.File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        // Cleanup
        file.delete();
    }

    @Test
    void showFundmovement() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();

        // Act & Assert
        assertDoesNotThrow(() -> transaction.showFundmovement("SenderUser"));
    }
}
