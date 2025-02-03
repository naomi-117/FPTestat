package org.example;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Transaction {
    private int transactionId;
    private double amount;
    private String description;
    private String receiver;
    private String transmitter;
    private Date date;

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getTransmitter() {
        return transmitter;
    }

    public void setTransmitter(String transmitter) {
        this.transmitter = transmitter;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void singleTransaction(double amount, String description, String transmitterUsername, String receiverUsername) {
        Connection conn = null;
        PreparedStatement checkMoneyS = null;
        PreparedStatement transactionS = null;
        ResultSet resultS = null;

        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Transmitter ID ermitteln
            String transmitterQuery = "SELECT User_ID, Konto_guthaben FROM User WHERE Username = ?";
            checkMoneyS = conn.prepareStatement(transmitterQuery);
            checkMoneyS.setString(1, transmitterUsername);
            resultS = checkMoneyS.executeQuery();

            if (!resultS.next()) {
                System.out.println("Sender wurde nicht gefunden.");
                conn.rollback();
                return;
            }
            int transmitterId = resultS.getInt("User_ID");
            double currentBalance = resultS.getDouble("Konto_guthaben");
            resultS.close();
            checkMoneyS.close();

            if (currentBalance < amount) {
                System.out.println("Nicht genügend Guthaben. Die Transaktion wurde abgebrochen.");
                conn.rollback();
                return;
            }

            // Receiver ID ermitteln
            String receiverQuery = "SELECT User_ID FROM User WHERE Username = ?";
            checkMoneyS = conn.prepareStatement(receiverQuery);
            checkMoneyS.setString(1, receiverUsername);
            resultS = checkMoneyS.executeQuery();

            if (!resultS.next()) {
                System.out.println("Empfänger wurde nicht gefunden.");
                conn.rollback();
                return;
            }
            int receiverId = resultS.getInt("User_ID");
            resultS.close();
            checkMoneyS.close();

            // Guthaben vom Sender abziehen
            String deductQuery = "UPDATE User SET Konto_guthaben = Konto_guthaben - ? WHERE User_ID = ?";
            transactionS = conn.prepareStatement(deductQuery);
            transactionS.setDouble(1, amount);
            transactionS.setInt(2, transmitterId);
            transactionS.executeUpdate();

            // Guthaben zum Empfänger hinzufügen
            String addQuery = "UPDATE User SET Konto_guthaben = Konto_guthaben + ? WHERE User_ID = ?";
            transactionS = conn.prepareStatement(addQuery);
            transactionS.setDouble(1, amount);
            transactionS.setInt(2, receiverId);
            transactionS.executeUpdate();

            // Transaktion einfügen
            String insertTransaction = "INSERT INTO Transaktionen (Betrag, Beschreibung, Sender, Empfaenger, Datum) " +
                    "VALUES (?, ?, ?, ?, CURRENT_DATE)";
            transactionS = conn.prepareStatement(insertTransaction);
            transactionS.setDouble(1, amount);
            transactionS.setString(2, description);
            transactionS.setInt(3, transmitterId);
            transactionS.setInt(4, receiverId);
            transactionS.executeUpdate();

            conn.commit();
            System.out.println("Die Transaktion war erfolgreich.");
        } catch (SQLException e) {
            System.err.println("Fehler während der Transaktion: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Fehler beim Zurücksetzen der Transaktion: " + rollbackEx.getMessage());
            }
        } finally {
            closeResources(conn, checkMoneyS, resultS);
            closeResources(conn, transactionS, null);
        }
    }


    public void payOutMoney(double amount, String username) {
        Connection conn = null;
        PreparedStatement checkMoneyS = null;
        PreparedStatement transactionS = null;
        ResultSet resultS = null;

        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Benutzer-Guthaben überprüfen
            String balanceQuery = "SELECT Konto_guthaben FROM User WHERE Username = ?";
            checkMoneyS = conn.prepareStatement(balanceQuery);
            checkMoneyS.setString(1, username);
            resultS = checkMoneyS.executeQuery();

            if (!resultS.next()) {
                System.out.println("Benutzer wurde nicht gefunden.");
                conn.rollback();
                return;
            }
            double currentBalance = resultS.getDouble("Konto_guthaben");

            if (currentBalance < amount) {
                System.out.println("Nicht genügend Guthaben. Die Auszahlung wurde abgebrochen.");
                conn.rollback();
                return;
            }

            // Guthaben reduzieren
            String deductQuery = "UPDATE User SET Konto_guthaben = Konto_guthaben - ? WHERE Username = ?";
            transactionS = conn.prepareStatement(deductQuery);
            transactionS.setDouble(1, amount);
            transactionS.setString(2, username);
            transactionS.executeUpdate();

            // Transaktion hinzufügen
            String insertTransaction = "INSERT INTO Transaktionen (Betrag, Beschreibung, Sender, Empfaenger, Datum) " +
                    "VALUES (?, NULL, (SELECT User_ID FROM User WHERE Username = ?), NULL, CURRENT_DATE)";
            transactionS = conn.prepareStatement(insertTransaction);
            transactionS.setDouble(1, amount);
            transactionS.setString(2, username);
            transactionS.executeUpdate();

            conn.commit();
            System.out.println("Die Auszahlung war erfolgreich.");
        } catch (SQLException e) {
            System.err.println("Fehler während der Auszahlung: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Fehler beim Zurücksetzen der Auszahlung: " + rollbackEx.getMessage());
            }
        } finally {
            closeResources(conn, checkMoneyS, resultS);
            closeResources(conn, transactionS, null);
        }
    }


    public void uploadCSVForBulkTransfer(String filePath) {
        Connection conn = null;
        PreparedStatement checkUserStmt = null;
        ResultSet resultS = null;

        List<String> invalidLines = new ArrayList<>();
        double totalAmount = 0.0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                String[] parts = line.split(";");
                if (parts.length != 3) {
                    invalidLines.add("Zeile " + lineNumber + ": Ungültige Formatierung");
                    continue;
                }

                String receiver = parts[0].trim();
                double amount;
                String description = parts[2].trim();

                try {
                    amount = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {
                    invalidLines.add("Zeile " + lineNumber + ": Betrag ist keine gültige Zahl");
                    continue;
                }

                if (amount <= 0) {
                    invalidLines.add("Zeile " + lineNumber + ": Betrag muss größer als 0 sein");
                    continue;
                }

                String userCheckQuery = "SELECT User_ID FROM User WHERE Username = ?";
                checkUserStmt = conn.prepareStatement(userCheckQuery);
                checkUserStmt.setString(1, receiver);
                resultS = checkUserStmt.executeQuery();

                if (!resultS.next()) {
                    invalidLines.add("Zeile " + lineNumber + ": User " + receiver + " existiert nicht");
                    continue;
                }

                totalAmount += amount;
            }

            if (!invalidLines.isEmpty()) {
                System.out.println("Die folgenden Zeilen haben Fehler:");
                for (String error : invalidLines) {
                    System.out.println(error);
                }
                conn.rollback();
                return;
            }

            String balanceQuery = "SELECT Konto_guthaben FROM User WHERE User_ID = ?";
            PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery);
            balanceStmt.setString(1, transmitter);
            resultS = balanceStmt.executeQuery();

            if (resultS.next()) {
                double currentBalance = resultS.getDouble("Konto_guthaben");

                if (currentBalance < totalAmount) {
                    System.out.println("Massenüberweisung fehlgeschlagen: Nicht genügend Guthaben.");
                    conn.rollback();
                    return;
                }
            }

            br.close();
            try (BufferedReader br2 = new BufferedReader(new FileReader(filePath))) {
                while ((line = br2.readLine()) != null) {
                    String[] parts = line.split(";");
                    String receiver = parts[0].trim();
                    double amount = Double.parseDouble(parts[1].trim());
                    String description = parts[2].trim();

                    executeSingleTransaction(conn, amount, description, receiver);
                }
            }

            conn.commit();
            System.out.println("Massenüberweisung erfolgreich abgeschlossen.");

        } catch (IOException | SQLException e) {
            System.err.println("Fehler während der Verarbeitung: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Fehler beim Zurücksetzen der Transaktion: " + rollbackEx.getMessage());
            }
        } finally {
            closeResources(conn, checkUserStmt, resultS);
        }
    }

    private void executeSingleTransaction(Connection con, double amount, String description, String receiver) throws SQLException {
        String deductQuery = "UPDATE User SET Konto_guthaben = Konto_guthaben - ? WHERE User_ID = ?";
        PreparedStatement deductStmt = con.prepareStatement(deductQuery);
        deductStmt.setDouble(1, amount);
        deductStmt.setString(2, transmitter);
        deductStmt.executeUpdate();

        String addQuery = "UPDATE User SET Konto_guthaben = Konto_guthaben + ? WHERE User_ID = ?";
        PreparedStatement addStmt = con.prepareStatement(addQuery);
        addStmt.setDouble(1, amount);
        addStmt.setString(2, receiver);
        addStmt.executeUpdate();

        String insertTransaction = "INSERT INTO Transaktionen (Betrag, Beschreibung, Sender_ID, Empfänger_ID, Datum) " +
                "VALUES (?, ?, ?, ?, CURRENT_DATE)";
        PreparedStatement insertStmt = con.prepareStatement(insertTransaction);
        insertStmt.setDouble(1, amount);
        insertStmt.setString(2, description);
        insertStmt.setString(3, transmitter);
        insertStmt.setString(4, receiver);
        insertStmt.executeUpdate();
    }

    public void exportTransactions(String username, String filePath) {
        Connection conn = null;
        PreparedStatement exportStmt = null;
        ResultSet resultS = null;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            conn = DatabaseConnection.getInstance().getConnection();

            // SQL-Abfrage, um alle Transaktionen des Benutzers zu exportieren
            String exportQuery = "SELECT t.Betrag, t.Beschreibung, t.Datum, " +
                    "(SELECT Username FROM User WHERE User_ID = t.Sender) AS SenderName, " +
                    "(SELECT Username FROM User WHERE User_ID = t.Empfaenger) AS EmpfaengerName " +
                    "FROM Transaktionen t " +
                    "WHERE t.Sender = (SELECT User_ID FROM User WHERE Username = ?) " +
                    "OR t.Empfaenger = (SELECT User_ID FROM User WHERE Username = ?) " +
                    "ORDER BY t.Datum DESC";

            exportStmt = conn.prepareStatement(exportQuery);
            exportStmt.setString(1, username);
            exportStmt.setString(2, username);

            resultS = exportStmt.executeQuery();

            writer.write("Exportierte Transaktionen für Benutzer: " + username + "\n\n");

            boolean hasTransactions = false;
            while (resultS.next()) {
                hasTransactions = true;
                double betrag = resultS.getDouble("Betrag");
                String beschreibung = resultS.getString("Beschreibung");
                Timestamp datum = resultS.getTimestamp("Datum");
                String senderName = resultS.getString("SenderName");
                String empfaengerName = resultS.getString("EmpfaengerName");

                writer.write("Datum: " + datum + "\n");
                writer.write("Betrag: " + betrag + "\n");
                writer.write("Beschreibung: " + (beschreibung != null ? beschreibung : "Keine Beschreibung") + "\n");
                writer.write("Von: " + senderName + "\n");
                writer.write("An: " + empfaengerName + "\n");
                writer.write("------------------------------------\n");
            }

            if (!hasTransactions) {
                writer.write("Keine Transaktionen gefunden.");
            }

            System.out.println("Transaktionen erfolgreich in die Datei " + filePath + " exportiert.");

        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der Transaktionen: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        } finally {
            closeResources(conn, exportStmt, resultS);
        }
    }

    public void showFundmovement(String username) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet resultS = null;

        try {
            conn = DatabaseConnection.getInstance().getConnection();

            // SQL-Abfrage, um Geldbewegungen des Benutzers zu holen
            String query = "SELECT t.Betrag, t.Beschreibung, t.Datum, " +
                    "(SELECT Username FROM User WHERE User_ID = t.Sender) AS SenderName, " +
                    "(SELECT Username FROM User WHERE User_ID = t.Empfaenger) AS EmpfaengerName " +
                    "FROM Transaktionen t " +
                    "WHERE t.Sender = (SELECT User_ID FROM User WHERE Username = ?) " +
                    "OR t.Empfaenger = (SELECT User_ID FROM User WHERE Username = ?) " +
                    "ORDER BY t.Datum DESC";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, username);

            resultS = stmt.executeQuery();

            System.out.println("Geldbewegungen für Benutzer: " + username);
            System.out.println("------------------------------------");

            boolean hasTransactions = false;
            while (resultS.next()) {
                hasTransactions = true;
                double betrag = resultS.getDouble("Betrag");
                String beschreibung = resultS.getString("Beschreibung");
                Timestamp datum = resultS.getTimestamp("Datum");
                String senderName = resultS.getString("SenderName");
                String empfaengerName = resultS.getString("EmpfaengerName");

                System.out.println("Datum: " + datum);
                System.out.println("Betrag: " + betrag);
                System.out.println("Beschreibung: " + (beschreibung != null ? beschreibung : "Keine Beschreibung"));
                System.out.println("Von: " + senderName);
                System.out.println("An: " + empfaengerName);
                System.out.println("------------------------------------");
            }

            if (!hasTransactions) {
                System.out.println("Keine Geldbewegungen gefunden.");
            }

        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der Geldbewegungen: " + e.getMessage());
        } finally {
            closeResources(conn, stmt, resultS);
        }
    }

    private void closeResources(Connection con, PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (con != null) con.close();
        } catch (SQLException e) {
            System.err.println("Fehler beim Schließen der Ressourcen: " + e.getMessage());
        }
    }
}

