package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class User {
    Scanner scanner = new Scanner(System.in);

    private int userId;
    private String username;
    private String password;
    private boolean hasAccount;
    private double Balance;
    private boolean hasTransaction;
    private String pinboardMessage;
    private String directMessage;

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean hasAccount() {
        return hasAccount;
    }

    public double getBalance() {
        return Balance;
    }

    public boolean hasTransaction() {
        return hasTransaction;
    }

    public String getPinboardMessage() {
        return pinboardMessage;
    }

    public String getDirectMessage() {
        return directMessage;
    }

    public void registerUser() {
        Connection conn = null;
        PreparedStatement statment = null;
        ResultSet rs = null;

        while (true) {
            System.out.println("Geben Sie Ihr Benutzernamen ein: ");
            username = scanner.next();

            if (isValidEmail(username)) {
                break;
            }

            System.out.println("Das ist keine gültige E-Mail-Adresse. Bitte versuchen Sie es erneut.");
        }

        System.out.println("Geben Sie ein neues Passwort ein: ");
        String password = scanner.next();

        try {
            conn = DatabaseConnection.getInstance().getConnection();
            String query = "SELECT Username FROM User WHERE Username = ?";
            statment = conn.prepareStatement(query);
            statment.setString(1, username);
            rs = statment.executeQuery();

            if (rs.next()) {
                System.out.println("Benutzername gib es bereits");
                conn.rollback();
                return;
            }

            conn = DatabaseConnection.getInstance().getConnection();
            // String query1 = "INSERT INTO \"User\" (Username, Password, Konto, Guthaben, Transaction, Pinboard_Message, Direktnachricht) " +
                    "VALUES (?, ?, TRUE, 1000, NULL, NULL, NULL)";
            statment = conn.prepareStatement(query1);
            statment.setString(1, username);
            statment.setString(2, password);
            rs = statment.executeQuery();

            if (rs.next()) {
                System.out.println("Sie sind erfolgreich registriert");
            }

        } catch (SQLException e) {
            System.err.println("Fehler beim Registieren: " + e.getMessage());
        } finally {
            closeResources(conn, statment, rs);
        }
    }

    boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,3}$";
        return email.matches(emailRegex);
    }

    public void loginUser() {
        Connection conn = null;
        PreparedStatement statment = null;
        ResultSet rs = null;

        while (true) {
            System.out.println("Geben Sie Ihr Benutzernamen ein: ");
            username = scanner.next();

            if (isValidEmail(username)) {
                break;
            }

            System.out.println("Das ist keine gültige E-Mail-Adresse. Bitte versuchen Sie es erneut.");
        }

        System.out.println("Geben Sie Ihr Passwort ein: ");
        String password = scanner.next();

        try {
            conn = DatabaseConnection.getInstance().getConnection();
            String query = "SELECT Username, Password FROM User WHERE Username = ? AND Password = ?";
            statment = conn.prepareStatement(query);
            statment.setString(1, username);
            statment.setString(2, password);
            rs = statment.executeQuery();

            if (rs.next()) {
                System.out.println("Sie sind erfolgreich eingelogt");
            } else {
                System.out.println("Die Anmeldedaten sind falsch");
            }

        } catch (SQLException e) {
            System.err.println("Fehler beim Anmelden: " + e.getMessage());
        } finally {
            closeResources(conn, statment, rs);
        }

    }
  
    public void showCredit() {
        Connection conn = null;
        PreparedStatement statment = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            String query = "SELECT Konto_guthaben FROM User WHERE Username = ?";
            statment = conn.prepareStatement(query);
            statment.setString(1, username);
            rs = statment.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("Konto_guthaben");
                System.out.println("Ihr aktuelles Guthaben beträgt: €" + balance);
            } else {
                System.out.println("User wurde nicht gefunden.");
            }
        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen des Guthabens: " + e.getMessage());
        } finally {
            closeResources(conn, statment, rs);
        }
    }

    private void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("Fehler beim Schließen der Ressourcen: " + e.getMessage());
        }
    }
}