package org.example;

import java.util.Objects;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        User user = new User();
        Transaction tranact = new Transaction();
        Pinwand_und_Nachrichten pinwandUndNachrichten = new Pinwand_und_Nachrichten();

        System.out.println("Wollen sie sich Registrieren oder Anmelden?");
        System.out.println("1 Registrieren \n2 Anmelden");
        String stringDec = null;
        String stringDec1 = null;
        String stringDec2 = null;
        Double doubleDec = null;
        int intDec = 0;
        int intDec1 = 0;
        stringDec = scan.next();
        if(Objects.equals(stringDec, "1")) {
            user.registerUser();
        }
        else if(Objects.equals(stringDec, "2")) {
            user.loginUser();
        }
        else {
            try {
                System.out.println("Wollen sie sich Registrieren oder Anmelden? \nAchten sie auf die notwendige Auwahl!");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Was wollen sie machen?");
        System.out.println("1 Guthaben einsehen \n2 Einzehltransaktion durchführen\n3 CSV Datei hochladen für Massenüberweisung \n4 Geld auszahlen " +
                "\n5 Kommentar an Pinnwand schreiben \n6 Eigene Pinnwand einsehen \n7 Benutzer suchen \n8 Direktnachrichten abrufen " +
                "\n9 Direktnachrichten exportieren \n10 Geldtransaktionen exportieren \n11 Geldbewegung einsehen");
        stringDec = scan.next();

        switch (stringDec) {
            case "1":
                user.showCredit();
                break;
            case "2":
                System.out.println("Zu wem wollen sie Geld senden?");
                stringDec = scan.next();
                tranact.setReceiver(stringDec);
                System.out.println("Wie viel Geld wollen sie senden?");
                doubleDec = scan.nextDouble();
                System.out.println("Geben sie eine Beschreibung an:");
                stringDec1 = scan.next();
                tranact.singleTransaction(doubleDec, stringDec1, user.getUsername(), stringDec);
                break;
            case "3":
                System.out.println("Geben sie die CSV Datei für die Massenüberweisung an:");
                stringDec = scan.next();
                tranact.uploadCSVForBulkTransfer(stringDec);
                break;
            case "4":
                System.out.println("Wie viel Geld wollen sie auszahlen?");
                doubleDec = scan.nextDouble();
                tranact.payOutMoney(doubleDec, user.getUsername());
                break;
            case "5":
                System.out.print("Empfänger Benutzername: ");
                stringDec1 = scan.next();

                System.out.print("Ihre Nachricht: ");
                stringDec = scan.nextLine();

                pinwandUndNachrichten.writeCommentOnPinboard(user.getUsername(), stringDec1, stringDec);
                break;
            case "6":
                pinwandUndNachrichten.showPinboard(user.getUsername());
                break;
            case "7":
                System.out.println("Wenn wollen sie suchen?");
                stringDec = scan.next();
                pinwandUndNachrichten.searchUser(stringDec);
                break;
            case "8":
                pinwandUndNachrichten.showDirectMessages(user.getUsername());
                break;
            case "9":
                System.out.println("Zwischem welchem Kontakt sollen ihre Nachrichten exportiert werden?");
                stringDec = scan.next();
                System.out.println("Wollen Sie Pinnwand oder Direktnachrichten exportieren?");
                stringDec1 = scan.next();
                System.out.println("Wohin soll die Datei gespeichert werden: (Geben sie einen Pfad an)");
                stringDec2 = scan.next();
                pinwandUndNachrichten.exportMessages(user.getUsername(), stringDec, stringDec1, stringDec2);
                break;
            case "10":
                System.out.println("Wohin soll die Datei gespeichert werden: (Geben sie einen Pfad an)");
                stringDec = scan.next();
                tranact.exportTransactions(user.getUsername(), stringDec);
                break;
            case "11":
                tranact.showFundmovement(user.getUsername());
                break;
            default:
                try {
                    System.out.println("Was wollen sie machen? \nAchten sie auf die notwendige Auswahl!");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
        }
        scan.close();
    }
}