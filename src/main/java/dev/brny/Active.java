package dev.brny;

import java.net.*;
import java.io.*;
import java.util.Objects;
import java.util.Scanner;


public class Active {
    private Socket c;
    private PrintWriter out;
    private BufferedReader in;

    public void connect(String ip, int port, String username, Router router, MessageHandler msg) throws IOException {


        c = new Socket(ip, port);
        out = new PrintWriter(c.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        System.out.println("[WAIT] Connected successfully, verifying handshake");
        out.println(msg.encode(Protocol.header));
        String handshake = msg.decode(in.readLine());
        if (Objects.equals(handshake, Protocol.header)) {
            System.out.println("[OK] Handshake OK, Exchanging keys");
            out.println(msg.encode(msg.get_key()));
            String received_key = msg.decode(in.readLine());
            try {
                msg.set_curr_key(received_key);
            } catch (Exception e) {
                System.err.println("[ERROR] " + e + " Stopping connection, peer not trusted");
                stop();
            }
            System.out.println("[OK] Handshake verified, encryption successful");
            System.out.println("[OK] Sending nickname: " + username);
            out.println(msg.encode(msg.encrypt(username)));
            System.out.println("[WAIT] Waiting for peer nickname");
            String other_user = msg.decrypt(msg.decode(in.readLine()));
            System.out.println("[OK] Received peer nickname: " + other_user);
            router.setCurr_peer_ip(ip);
            router.add_peer(ip, other_user);
            router.add_peer(c.getInetAddress().getHostAddress(), username);
            new Thread(() -> {
                try {
                    String incoming;
                    while ((incoming = in.readLine()) != null) {
                        try {
                            String cleaned = msg.cleanup_msg(incoming);
                            if (cleaned == null || cleaned.isEmpty()) {
                                continue;
                            }
                            if (cleaned.equals("DEC_FAIL_ERR")) {
                                System.err.println("\r[WARN] Failed to decrypt incoming message. You may be a victim to a man in the middle attack.");
                                System.out.print("> ");
                                continue;
                            }
                            System.out.println("\r(" + other_user + ") " + cleaned + "      ");
                            System.out.print("> ");
                        } catch (Exception e) {
                            System.err.println("[ERROR] " + e);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[ERROR] Connection lost with peer");
                }
            }
            ).start();
            Scanner scan = new Scanner(System.in);
            while (true) {
                System.out.print(">");
                String to_send = scan.nextLine();
                if (Objects.equals(to_send, "/exit")) {
                    out.println(msg.wrap_msg("Peer left"));
                    stop();
                    break;
                }
                out.println(msg.wrap_msg(to_send));
            }
        } else {
            System.err.println("[ERROR] Handshake mismatch!");
            stop();
        }
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        c.close();
    }
}
