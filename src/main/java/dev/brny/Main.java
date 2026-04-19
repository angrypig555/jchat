package dev.brny;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;


public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        Passive server = new Passive();
        Active client = new Active();
        Router router = new Router();
        MessageHandler msg = new MessageHandler();
        FileHandler fh = new FileHandler();
        System.out.println("jchat V1.0\n");
        System.out.print("Please enter nickname: ");
        String nick = scan.nextLine();
        Log.get().log(Level.INFO, "Nick " + nick);
        router.start_router();
        try {
            msg.crypt_init();
        } catch (GeneralSecurityException e) {
            System.err.println("[ERROR] Failed to initialize keys! " + e);
            Log.get().log(Level.SEVERE, "Failed to initialize keys! " + e);
            return;
        }
        while (true) {
            System.out.println("Would you like to\n1. connect to someone\n2. someone to connect to you?\n3. Discover other peers via router\n4. Host a file\n 5. Download a file\n 6. Exit");
            String input = scan.nextLine();
            int number = 1;
            try {
                number = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                Log.get().log(Level.WARNING, "User entered not a number, defaulting to option 1");
                number = 1;
            }
            if (number == 1) {
                System.out.print("Please enter IP of peer: ");
                String peer_ip = scan.nextLine();
                Log.get().log(Level.INFO, "Connecting to " + peer_ip);
                try {

                    client.connect(peer_ip, 5400, nick, router, msg);
                } catch (IOException e) {
                    System.err.println("[ERROR] Networking error! " + e);
                    Log.get().log(Level.SEVERE, "Networking error! " + e);
                }
            } else if (number == 2) {
                System.out.println("[WAIT] Starting in passive mode...");
                Log.get().log(Level.INFO, "Starting in passive mode");
                try {
                    server.start(5400, nick, router, msg);
                } catch (IOException e) {
                    System.err.println("[ERROR] Networking error! " + e);
                    Log.get().log(Level.SEVERE, "Networking error!" + e);
                }
            } else if (number == 3) {
                try {
                    String address = router.print_peers();
                    if (Objects.equals(address, "no")) {
                        System.out.println("[WARNING] No known peers, currently in bootstrap mode. Please enter an ip of a known peer to get a list of IP's");
                        Log.get().log(Level.INFO, "No known peers, currently in bootstrap mode");
                        String toscan = scan.nextLine();
                        router.request_data(toscan);
                        System.out.println("[INFO] Please choose option 3 again to connect to an IP via router");
                    } else {
                        try {
                            client.connect(address, 5400, nick, router, msg);
                        } catch (IOException e) {
                            System.err.println("[ERROR] Networking error! " + e);
                            Log.get().log(Level.SEVERE, "Networking error! " + e);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[ROUTER] Networking error! " + e);
                    Log.get().log(Level.SEVERE, "Networking error! " + e);
                }
            } else if (number == 4) {
                System.out.print("Please enter path to file (e.g C:... or /home/foo/... ): ");
                String path_in = scan.nextLine();
                Log.get().log(Level.INFO, "Sharing file at " + path_in);
                File path = new File(path_in);
                fh.start_share(path);
            } else if (number == 5) {
              System.out.print("Please enter directory to save to (e.g C:... or /home/foo/...): ");
              String path_in = scan.nextLine();
              System.out.print("Please enter hash of file: ");
              String hash_raw = scan.nextLine();
                Log.get().log(Level.INFO, "Saving to " + path_in + " hash " + hash_raw);
                try {
                    fh.get_data(router, hash_raw, path_in);
                } catch (IOException e) {
                    System.err.println("[FILE] Error: " + e);
                    Log.get().log(Level.SEVERE, "File error " + e);
                }
            } else {
                return;
            }
        }
    }
}