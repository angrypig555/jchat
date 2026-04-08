package dev.brny;
import java.io.IOException;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        Passive server = new Passive();
        Active client = new Active();
        Router router = new Router();
        System.out.println("jchat V0.3\n");
        System.out.print("Please enter nickname: ");
        String nick = scan.nextLine();
        router.start_router();
        while (true) {
            System.out.println("Would you like to\n1. connect to someone\n2. someone to connect to you?\n3. exit");
            String input = scan.nextLine();
            int number = 1;
            try {
                number = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                number = 1;
            }
            if (number == 1) {
                System.out.print("Please enter IP of peer: ");
                String peer_ip = scan.nextLine();
                try {
                    client.connect(peer_ip, 5400, nick);
                } catch (IOException e) {
                    System.err.println("[ERROR] Networking error!" + e);
                    break; // BUG : connection fail = hammering. Fixed!
                }
            } else if (number == 2) {
                System.out.println("[WAIT] Starting in passive mode...");
                try {
                    server.start(5400, nick);
                } catch (IOException e) {
                    System.err.println("[ERROR] Networking error! " + e);
                    break; // BUG : connection fail = hammering. Fixed! again..
                }
            } else {
                System.out.println("That is not an option!")
                return;
            }
        }
    }
}