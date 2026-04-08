package dev.brny;

import java.net.*;
import java.io.*;
import java.util.Objects;
import java.util.Scanner;

public class Active {
    private Socket c;
    private PrintWriter out;
    private BufferedReader in;

    public void connect(String ip, int port, String username) throws IOException {
        c = new Socket(ip, port);
        out = new PrintWriter(c.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        System.out.println("[WAIT] Connected successfully, verifying handshake");
        out.println(Protocol.header);
        String handshake = in.readLine();
        if (Objects.equals(handshake, Protocol.header)) {
            System.out.println("[OK] Handshake OK, This is UNENCRYPTED TRAFFIC");
            System.out.println("[OK] Sending nickname: " + username);
            out.println(username);
            System.out.println("[WAIT] Waiting for peer nickname");
            String other_user = in.readLine();
            System.out.println("[OK] Received peer nickname: " + other_user);
            new Thread(() -> {
                try {
                    String incoming;
                    while ((incoming = in.readLine()) != null) {
                        String cleaned = cleanup_msg(incoming);
                        System.out.println("\r(" + other_user + ") " + cleaned + "     \n> ");
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
            System.err.println("[ERROR] Handshake mismatch!");
            stop();
        }
    }
    // will be relocated soon
    @Deprecated(forRemoval = true, since = "V0.2")
    public String cleanup_msg(String message) {
        int headerLen = Protocol.header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }
    @Deprecated(forRemoval = true, since = "V0.2")
    public String wrap_msg(String message) {
        return Protocol.header + message + Protocol.header;
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        c.close();
    }
}
