package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private Connection testConnection;

    @BeforeEach
    void setupDatabase() throws Exception {
        testConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/FBTestat", "postgres", "1234");

        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("CREATE TABLE Benutzer (" +
                    "Benutzer_ID INT AUTO_INCREMENT PRIMARY KEY, " +
                    "Benutzername VARCHAR(50), " +
                    "Passwort VARCHAR(50), " +
                    "Konto BOOLEAN, " +
                    "Guthaben DOUBLE DEFAULT 0);");
        }
    }

    @AfterEach
    void teardownDatabase() throws Exception {
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("DROP TABLE Benutzer;");
        }
        testConnection.close();
    }

    @Test
    void registerUser() throws Exception {
        // Arrange
        User user = new User();
        String username = "testuser@example.com";
        String password = "securepassword";

        // Act
        user.registerUser();

        // Assert
        try (Statement stmt = testConnection.createStatement()) {
            var rs = stmt.executeQuery("SELECT * FROM Benutzer WHERE Benutzername = 'testuser@example.com';");
            assertTrue(rs.next());
            assertEquals(username, rs.getString("Benutzername"));
            assertEquals(password, rs.getString("Passwort"));
        }
    }

    @Test
    void isValidEmail() {
        // Arrange
        User user = new User();

        // Act & Assert
        assertTrue(user.isValidEmail("valid@example.com"));
        assertFalse(user.isValidEmail("invalid-email"));
        assertFalse(user.isValidEmail("missing@domain"));
    }

    @Test
    void loginUser() throws Exception {
        // Arrange
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("INSERT INTO Benutzer (Benutzername, Passwort, Konto) VALUES ('testuser@example.com', 'securepassword', TRUE);");
        }

        User user = new User();

        // Act & Assert
        assertDoesNotThrow(() -> user.loginUser());
    }

    @Test
    void showCredit() throws Exception {
        // Arrange
        try (Statement stmt = testConnection.createStatement()) {
            stmt.execute("INSERT INTO Benutzer (Benutzername, Passwort, Konto, Guthaben) VALUES ('testuser@example.com', 'securepassword', TRUE, 500.0);");
        }

        User user = new User();

        // Act
        user.showCredit();
    }
}
