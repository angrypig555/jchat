package dev.brny;

import java.net.*;
import java.io.*;
import java.util.Objects;
import java.util.Scanner;

import dev.brny.Protocol;

// This is the class for when the user is waiting for somebody to connect to them.

public class Passive {
    private ServerSocket s;
    private Socket c;
    private PrintWriter out;
    private BufferedReader in;

    public void start(int port, String username) throws IOException {
            System.out.println("[OK] Opening socket");
            s = new ServerSocket(port);
            c = s.accept();
            System.out.println("[OK] Awaiting connection");
            out = new PrintWriter(c.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String handshake = in.readLine();
            if (Objects.equals(handshake, Protocol.header)) {
                System.out.println("[WAIT] Handshake received, veryfing with peer. IP: " + c.getInetAddress().getHostAddress());
                out.println(Protocol.header);
                System.out.println("[OK] Handshake OK, This is UNENCRYPTED TRAFFIC");
                System.out.println("[WAIT] Waiting for peer nickname");
                String other_user = in.readLine();
                System.out.println("[OK] Received peer nickname: " + other_user);
                System.out.println("[OK] Sending nickname: " + username);
                out.println(username);
                new Thread(() -> {
                    try {
                        String incoming;
                        while ((incoming = in.readLine()) != null) {
                            String cleaned = cleanup_msg(incoming);
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
                        out.println(wrap_msg("Peer left"));
                        stop();
                        break;
                    }
                    out.println(wrap_msg(to_send));
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
    public String cleanup_msg(String message) {
        int headerLen = Protocol.header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }

    public String wrap_msg(String message) {
        return Protocol.header + message + Protocol.header;
    }
}
