package dev.brny;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        Passive server = new Passive();
        Active client = new Active();
        Router router = new Router();
        MessageHandler msg = new MessageHandler();
        System.out.println("jchat V0.5\n");
        System.out.print("Please enter nickname: ");
        String nick = scan.nextLine();
        router.start_router();
        try {
            msg.crypt_init();
        } catch (GeneralSecurityException e) {
            System.err.println("[ERROR] Failed to initialize keys!");
            return;
        }
        while (true) {
            System.out.println("Would you like to\n1. connect to someone\n2. someone to connect to you?\n3. Discover other peers via router\n4. exit");
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

                    client.connect(peer_ip, 5400, nick, router, msg);
                } catch (IOException e) {
                    System.err.println("[ERROR] Networking error!" + e);
                }
            } else if (number == 2) {
                System.out.println("[WAIT] Starting in passive mode...");
                try {
                    server.start(5400, nick, router, msg);
                } catch (IOException e) {
                    System.err.println("[ERROR] Networking error! " + e);
                }
            } else if (number == 3) {
                try {
                    String address = router.print_peers();
                    if (Objects.equals(address, "no")) {
                        System.out.println("[WARNING] No known peers, currently in bootstrap mode. Please enter an ip of a known peer to get a list of IP's");
                        String toscan = scan.nextLine();
                        router.request_data(toscan);
                        System.out.println("[INFO] Please choose option 3 again to connect to an IP via router");
                    } else {
                        try {
                            client.connect(address, 5400, nick, router, msg);
                        } catch (IOException e) {
                            System.err.println("[ERROR] Networking error! " + e);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[ROUTER] Networking error! " + e);
                }
            } else {
                return;
            }
        }
    }
}