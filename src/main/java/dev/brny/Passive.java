package dev.brny;

import java.net.*;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Scanner;


// This is the class for when the user is waiting for somebody to connect to them.

public class Passive {
    private ServerSocket s;
    private Socket c;
    private PrintWriter out;
    private BufferedReader in;

    public void start(int port, String username, Router router, MessageHandler msg) throws IOException {
            System.out.println("[OK] Opening socket");
            s = new ServerSocket(port);
            System.out.println("[INFO] IP: " + s.getInetAddress().getHostAddress());
            System.out.println("[OK] Awaiting connection");
            c = s.accept();
            out = new PrintWriter(c.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String handshake = msg.decode(in.readLine());
            if (Objects.equals(handshake, Protocol.header)) {
                System.out.println("[WAIT] Handshake received, veryfing with peer. IP: " + c.getInetAddress().getHostAddress());
                out.println(msg.encode(Protocol.header));
                System.out.println("[OK] Handshake OK, veryfing keys");
                String other_key = msg.decode(in.readLine());
                try {
                    msg.set_curr_key(other_key);
                } catch (GeneralSecurityException e) {
                    System.err.println("[ERROR] Error while verifying peers key! Dropping connection, peer untrusted! " + e);
                    stop();
                }
                out.println(msg.encode(msg.get_key()));
                System.out.println("[WAIT] Waiting for peer nickname");
                String other_user = msg.decrypt(msg.decode(in.readLine()));
                System.out.println("[OK] Received peer nickname: " + other_user);
                System.out.println("[OK] Sending nickname: " + username);
                out.println(msg.encode(msg.encrypt(username)));
                router.setCurr_peer_ip(c.getInetAddress().getHostAddress());
                router.add_peer(c.getInetAddress().getHostAddress(), other_user);
                router.add_peer(s.getInetAddress().getHostAddress(), username);
                new Thread(() -> {
                    try {
                        String incoming;
                        while ((incoming = in.readLine()) != null) {
                            String cleaned = msg.cleanup_msg(incoming);
                            System.out.println("\r("+ other_user + ") " + cleaned + "     \n> ");
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
                System.err.println("[ERROR] Invalid handshake received from client on IP: " + c.getInetAddress().getHostAddress());
                System.err.println("Expected: " + Protocol.header + "\nGot: " + handshake);
                stop();
            }

    }
    public void stop() throws IOException {
        in.close();
        out.close();
        c.close();
        s.close();
    }
}
