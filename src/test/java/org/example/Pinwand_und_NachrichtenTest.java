package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class Pinwand_und_NachrichtenTest {

    private Connection testConnection;

    @BeforeEach
    void setupDatabase() throws Exception {
        testConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/FBTestat", "postgres", "1234");

        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("CREATE TABLE Benutzer (Benutzer_ID INT AUTO_INCREMENT PRIMARY KEY, Benutzername VARCHAR(50));");
            stmt.execute("CREATE TABLE Pinnwand_Nachrichten (Nachricht_ID INT AUTO_INCREMENT PRIMARY KEY, Nachricht TEXT, Sender INT, Empfaenger INT, Zeitpunkt TIMESTAMP);");

            stmt.execute("INSERT INTO Benutzer (Benutzername) VALUES ('SenderUser');");
            stmt.execute("INSERT INTO Benutzer (Benutzername) VALUES ('ReceiverUser');");
            stmt.execute("INSERT INTO Pinnwand_Nachrichten (Nachricht, Sender, Empfaenger, Zeitpunkt) VALUES ('Hallo', 1, 2, CURRENT_TIMESTAMP);");
        }
    }

    @AfterEach
    void teardownDatabase() throws Exception {
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("DROP TABLE Pinnwand_Nachrichten;");
            stmt.execute("DROP TABLE Benutzer;");
        }
        testConnection.close();
    }

    @Test
    void showPinboard() throws Exception {
        // Arrange
        Pinwand_und_Nachrichten pinwand = new Pinwand_und_Nachrichten();

        // Act & Assert
        assertDoesNotThrow(() -> pinwand.showPinboard("ReceiverUser"));
    }

    @Test
    void writeCommentOnPinboard() throws Exception {
        // Arrange
        Pinwand_und_Nachrichten pinwand = new Pinwand_und_Nachrichten();

        // Act
        pinwand.writeCommentOnPinboard("SenderUser", "ReceiverUser", "Test Nachricht");

        // Assert
        try (Statement stmt = testConnection.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM Pinnwand_Nachrichten WHERE Nachricht = 'Test Nachricht';");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("count"));
        }
    }

    @Test
    void searchUser() throws Exception {
        // Arrange
        Pinwand_und_Nachrichten pinwand = new Pinwand_und_Nachrichten();

        // Act & Assert
        assertDoesNotThrow(() -> pinwand.searchUser("Sender"));
    }

    @Test
    void showDirectMessages() throws Exception {
        // Arrange
        Pinwand_und_Nachrichten pinwand = new Pinwand_und_Nachrichten();

        // Act & Assert
        assertDoesNotThrow(() -> pinwand.showDirectMessages("ReceiverUser"));
    }

    @Test
    void exportMessages() throws Exception {
        // Arrange
        Pinwand_und_Nachrichten pinwand = new Pinwand_und_Nachrichten();
        String filePath = "test_export.txt";

        // Act
        pinwand.exportMessages("SenderUser", "ReceiverUser", "Pinnwand", filePath);

        // Assert
        java.io.File file = new java.io.File(filePath);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        file.delete();
    }
}
