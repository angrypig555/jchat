package dev.brny;
import java.io.IOException;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        Passive server = new Passive();
        System.out.println("Would you like to\n1. connect to someone\n2. someone to connect to you?");
        String input = scan.nextLine();
        int number = 1;
        try {
            number = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            number = 1;
        }
        if (number == 1) {
            System.out.println("wip");
        } else {
            System.out.println("[WAIT] Starting in passive mode...");
            try {
                server.start(5400);
            } catch (IOException e) {
                System.err.println("[ERROR] Networking error! " + e);
            }
        }
    }
}