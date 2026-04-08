// The router runs on a separate thread, completely independently of the normal communication on port 5401
package dev.brny;

import java.net.*;
import java.io.*;
import dev.brny.Protocol;

import java.util.ArrayList;
import java.util.Objects;

public class Router {
    ArrayList<String> known_peers;
    ArrayList<String> peer_names;
    private ServerSocket s;
    private Socket c;
    private PrintWriter out;
    private BufferedReader in;

    public void start_router() {
        new Thread(() -> {
            try {
                route();
            } catch (IOException e) {
                System.err.println("[ROUTER] Router error! " + e);
            }
        }
        ).start();
    }

    public void route() throws IOException {
        MessageHandler msg = new MessageHandler();
        System.out.println("[ROUTER] Opening router socket");
        s = new ServerSocket(5401);
        while (true) {
            c = s.accept();
            System.out.println("[ROUTER] Verifying handshake; Router request from " + c.getInetAddress().getHostAddress());
            String handshake = in.readLine();
            if (Objects.equals(handshake, Protocol.router_header)) {
                out.println(Protocol.header);
                String response = in.readLine();
                if (Objects.equals(response, "OK")) {
                    System.out.println("[ROUTER] Sending " + c.getInetAddress().getHostAddress() + " known peers.");
                    out.println(Protocol.router_header);
                    out.println(known_peers.size());
                    for (String ip : known_peers) {
                        out.println(ip);
                        in.readLine();
                    }
                    out.println("NICK");
                    for (String nick : peer_names) {
                        out.println(nick);
                        in.readLine();
                    }
                    out.println(Protocol.router_header);
                }
            } else {
                System.err.println("[ROUTER] Invalid handshake\nExpected: " + Protocol.router_header + "\nGot: " + handshake);
                c.close();
            }
        }
    }
    public void request_data(String ip_address) throws IOException {
        System.out.println("[ROUTER] Requesting data from peer");
        c = new Socket(ip_address, 5401);
        out = new PrintWriter(c.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        System.out.println("[ROUTER] Sending handshake to " + c.getInetAddress().getHostAddress());
        out.println(Protocol.router_header);
        String response = in.readLine();
        if (Objects.equals(response, Protocol.router_header)) {
            out.println("OK");
            System.out.println("[ROUTER] Handshake verified, receiving peers");
            String first_header = in.readLine();
            if (!Objects.equals(first_header, Protocol.router_header)) {
                System.err.println("[ROUTER] Unexpected behaviour from peer, disconnecting");
                c.close();
                return;
            }
            String size_string = in.readLine();
            int size = Integer.parseInt(size_string);
            int nick_size = size;
            while (size > 0) {
                String read_data = in.readLine();
                if (known_peers.contains(read_data)) {
                    --size;
                    out.println("OK");
                    continue;
                }
                known_peers.add(read_data);
                out.println("OK");
                --size;
            }
            String separator = in.readLine();
            if (!Objects.equals(separator, "NICK")) {
                System.err.println("[ROUTER] Unexpected behaviour from peer, disconnecting.");
                c.close();
                return;
            }
            while (nick_size > 0) {
                String read_data = in.readLine();
                peer_names.add(read_data);
                out.println("OK");
                --nick_size;
            }
            String last_header = in.readLine();
            System.out.println("[ROUTER] Succesfully updated list of peers, disconnecting from peer.");
            c.close();
        }
    }
}
