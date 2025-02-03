package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Scanner;

public class Pinwand_und_Nachrichten {
    User user = new User();
    public void showPinboard(String username) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet resultS = null;

        try {
            conn = DatabaseConnection.getInstance().getConnection();

            // SQL-Query anpassen, um den Benutzernamen zu verwenden
            String query = "SELECT Nachricht, Sender, Zeitpunkt FROM Pinnwand_Nachrichten " +
                    "JOIN Benutzer ON Pinnwand_Nachrichten.Empfaenger = Benutzer.Benutzer_ID " +
                    "WHERE Benutzer.Benutzername = ? ORDER BY Zeitpunkt DESC";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            resultS = stmt.executeQuery();

            System.out.println("Ihre Pinnwand-Nachrichten:");
            System.out.println("------------------------------------");

            boolean hasMessages = false;
            while (resultS.next()) {
                hasMessages = true;
                String nachricht = resultS.getString("Nachricht");
                int senderId = resultS.getInt("Sender");
                Timestamp zeitpunkt = resultS.getTimestamp("Zeitpunkt");

                System.out.println("Von Benutzer-ID: " + senderId);
                System.out.println("Zeitpunkt: " + zeitpunkt);
                System.out.println("Nachricht: " + nachricht);
                System.out.println("------------------------------------");
            }

            if (!hasMessages) {
                System.out.println("Ihre Pinnwand ist leer.");
            }

        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der Pinnwand-Nachrichten: " + e.getMessage());
        } finally {
            closeResources(conn, stmt, resultS);
        }
    }

    public void writeCommentOnPinboard(String senderUsername, String receiverUsername, String message) {
        Connection conn = null;
        PreparedStatement insertStmt = null;
        PreparedStatement userCheckStmt = null;
        ResultSet resultS = null;

        try {
            conn = DatabaseConnection.getInstance().getConnection();

            // Überprüfen, ob der Empfänger existiert
            String userCheckQuery = "SELECT Benutzer_ID FROM Benutzer WHERE Benutzername = ?";
            userCheckStmt = conn.prepareStatement(userCheckQuery);
            userCheckStmt.setString(1, receiverUsername);
            resultS = userCheckStmt.executeQuery();

            if (!resultS.next()) {
                System.out.println("Fehler: Der Empfänger mit dem Benutzernamen \"" + receiverUsername + "\" existiert nicht.");
                return;
            }

            int receiverId = resultS.getInt("Benutzer_ID");

            // Sender-ID ermitteln
            String senderQuery = "SELECT Benutzer_ID FROM Benutzer WHERE Benutzername = ?";
            PreparedStatement senderStmt = conn.prepareStatement(senderQuery);
            senderStmt.setString(1, senderUsername);
            ResultSet senderResult = senderStmt.executeQuery();

            if (!senderResult.next()) {
                System.out.println("Fehler: Der Sender mit dem Benutzernamen \"" + senderUsername + "\" existiert nicht.");
                senderResult.close();
                senderStmt.close();
                return;
            }

            int senderId = senderResult.getInt("Benutzer_ID");
            senderResult.close();
            senderStmt.close();

            // Nachricht in die Pinnwand einfügen
            String insertQuery = "INSERT INTO Pinnwand_Nachrichten (Nachricht, Sender, Empfaenger, Zeitpunkt) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
            insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, message);
            insertStmt.setInt(2, senderId);
            insertStmt.setInt(3, receiverId);
            insertStmt.executeUpdate();

            System.out.println("Nachricht erfolgreich auf die Pinnwand von Benutzer \"" + receiverUsername + "\" geschrieben.");
        } catch (SQLException e) {
            System.err.println("Fehler bei der Datenbankoperation: " + e.getMessage());
        } finally {
            closeResources(conn, userCheckStmt, resultS);
            try {
                if (insertStmt != null) insertStmt.close();
            } catch (SQLException e) {
                System.err.println("Fehler beim Schließen der Ressourcen: " + e.getMessage());
            }
        }
    }

    public void searchUser(String searchUsername) {
        Connection conn = null;
        PreparedStatement searchStmt = null;
        ResultSet resultS = null;

        try {
            conn = DatabaseConnection.getInstance().getConnection();

            // SQL-Abfrage, um Benutzer basierend auf dem Benutzernamen zu suchen
            String searchQuery = "SELECT Benutzer_ID, Benutzername FROM Benutzer WHERE Benutzername LIKE ?";
            searchStmt = conn.prepareStatement(searchQuery);
            searchStmt.setString(1, "%" + searchUsername + "%");
            resultS = searchStmt.executeQuery();

            System.out.println("Suchergebnisse:");
            System.out.println("------------------------------------");

            boolean hasResults = false;
            while (resultS.next()) {
                hasResults = true;
                int userId = resultS.getInt("Benutzer_ID");
                String username = resultS.getString("Benutzername");

                System.out.println("Benutzername: " + username);
                System.out.println("------------------------------------");

                // Fragen ob die Pinnwand des Benutzers aufgerufen werden soll
                System.out.println("Möchten Sie die Pinnwand von " + username + " aufrufen? (ja/nein)");
                Scanner scanner = new Scanner(System.in);
                String choice = scanner.nextLine();

                if (choice.equalsIgnoreCase("ja")) {
                    showPinboard(username);

                    // Fragen ob eine Nachricht auf die Pinnwand zu geschrieben werden soll
                    System.out.println("Möchten Sie eine Nachricht auf die Pinnwand schreiben? (ja/nein)");
                    String writeChoice = scanner.nextLine();

                    if (writeChoice.equalsIgnoreCase("ja")) {
                        System.out.println("Bitte geben Sie Ihre Nachricht ein:");
                        String message = scanner.nextLine();
                        writeCommentOnPinboard(user.getUsername(), username, message);
                    }
                }
            }

            if (!hasResults) {
                System.out.println("Keine Benutzer gefunden, die den Suchbegriff enthalten.");
            }

        } catch (SQLException e) {
            System.err.println("Fehler bei der Benutzersuche: " + e.getMessage());
        } finally {
            closeResources(conn, searchStmt, resultS);
        }
    }

    public void showDirectMessages(String username) {
        Connection conn = null;
        PreparedStatement fetchStmt = null;
        ResultSet resultS = null;

        try {
            conn = DatabaseConnection.getInstance().getConnection();

            // SQL-Abfrage, um alle Direktnachrichten zu holen, in denen der Benutzer involviert ist
            String fetchQuery = "SELECT dm.Nachricht, dm.Sender, dm.Empfaenger, dm.Zeitpunkt, " +
                    "(SELECT Benutzername FROM Benutzer WHERE Benutzer_ID = dm.Sender) AS SenderName, " +
                    "(SELECT Benutzername FROM Benutzer WHERE Benutzer_ID = dm.Empfaenger) AS EmpfaengerName " +
                    "FROM Direktnachrichten dm " +
                    "JOIN Benutzer b ON b.Benutzer_ID = dm.Empfaenger OR b.Benutzer_ID = dm.Sender " +
                    "WHERE b.Benutzername = ? " +
                    "ORDER BY dm.Zeitpunkt DESC";

            fetchStmt = conn.prepareStatement(fetchQuery);
            fetchStmt.setString(1, username);
            resultS = fetchStmt.executeQuery();

            System.out.println("Ihr Posteingang für Direktnachrichten:");
            System.out.println("------------------------------------");

            boolean hasMessages = false;
            while (resultS.next()) {
                hasMessages = true;
                String nachricht = resultS.getString("Nachricht");
                String senderName = resultS.getString("SenderName");
                String empfaengerName = resultS.getString("EmpfaengerName");
                Timestamp zeitpunkt = resultS.getTimestamp("Zeitpunkt");

                System.out.println("Von: " + senderName);
                System.out.println("An: " + empfaengerName);
                System.out.println("Zeitpunkt: " + zeitpunkt);
                System.out.println("Nachricht: " + nachricht);
                System.out.println("------------------------------------");
            }

            if (!hasMessages) {
                System.out.println("Ihr Posteingang ist leer.");
            }

        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der Direktnachrichten: " + e.getMessage());
        } finally {
            closeResources(conn, fetchStmt, resultS);
        }
    }

    public void exportMessages(String username, String contactUsername, String messageType, String filePath) {
        Connection conn = null;
        PreparedStatement exportStmt = null;
        ResultSet resultS = null;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            conn = DatabaseConnection.getInstance().getConnection();

            String exportQuery = "";
            if (messageType.equalsIgnoreCase("Pinnwand")) {
                exportQuery = "SELECT pn.Nachricht, pn.Zeitpunkt, " +
                        "(SELECT Benutzername FROM Benutzer WHERE Benutzer_ID = pn.Sender) AS SenderName " +
                        "FROM Pinnwand_Nachrichten pn " +
                        "JOIN Benutzer b ON b.Benutzer_ID = pn.Empfaenger " +
                        "WHERE b.Benutzername = ? AND pn.Sender = (SELECT Benutzer_ID FROM Benutzer WHERE Benutzername = ?) " +
                        "ORDER BY pn.Zeitpunkt DESC";
            } else if (messageType.equalsIgnoreCase("Direktnachrichten")) {
                exportQuery = "SELECT dm.Nachricht, dm.Zeitpunkt, " +
                        "(SELECT Benutzername FROM Benutzer WHERE Benutzer_ID = dm.Sender) AS SenderName, " +
                        "(SELECT Benutzername FROM Benutzer WHERE Benutzer_ID = dm.Empfaenger) AS EmpfaengerName " +
                        "FROM Direktnachrichten dm " +
                        "WHERE (dm.Sender = (SELECT Benutzer_ID FROM Benutzer WHERE Benutzername = ?) AND " +
                        "dm.Empfaenger = (SELECT Benutzer_ID FROM Benutzer WHERE Benutzername = ?)) " +
                        "OR (dm.Sender = (SELECT Benutzer_ID FROM Benutzer WHERE Benutzername = ?) AND " +
                        "dm.Empfaenger = (SELECT Benutzer_ID FROM Benutzer WHERE Benutzername = ?)) " +
                        "ORDER BY dm.Zeitpunkt DESC";
            } else {
                System.out.println("Ungültiger Nachrichtentyp. Verwenden Sie \"Pinnwand\" oder \"Direktnachrichten\".");
                return;
            }

            exportStmt = conn.prepareStatement(exportQuery);
            exportStmt.setString(1, username);
            exportStmt.setString(2, contactUsername);
            if (messageType.equalsIgnoreCase("Direktnachrichten")) {
                exportStmt.setString(3, contactUsername);
                exportStmt.setString(4, username);
            }

            resultS = exportStmt.executeQuery();

            writer.write("Exportierte " + messageType + " zwischen " + username + " und " + contactUsername + ":\n\n");

            boolean hasMessages = false;
            while (resultS.next()) {
                hasMessages = true;
                String nachricht = resultS.getString("Nachricht");
                Timestamp zeitpunkt = resultS.getTimestamp("Zeitpunkt");
                String senderName = resultS.getString("SenderName");

                if (messageType.equalsIgnoreCase("Direktnachrichten")) {
                    String empfaengerName = resultS.getString("EmpfaengerName");
                    writer.write("Von: " + senderName + " An: " + empfaengerName + "\n");
                } else {
                    writer.write("Von: " + senderName + "\n");
                }
                writer.write("Zeitpunkt: " + zeitpunkt + "\n");
                writer.write("Nachricht: " + nachricht + "\n");
                writer.write("------------------------------------\n");
            }

            if (!hasMessages) {
                writer.write("Keine Nachrichten gefunden.");
            }

            System.out.println("Nachrichten erfolgreich in die Datei " + filePath + " exportiert.");

        } catch (SQLException e) {
            System.err.println("Fehler beim Abrufen der Nachrichten: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        } finally {
            closeResources(conn, exportStmt, resultS);
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


