// The router runs on a separate thread, completely independently of the normal communication on port 5401
package dev.brny;

import java.net.*;
import java.io.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class Router {
    ArrayList<String> known_peers = new ArrayList<>();
    ArrayList<String> peer_names = new ArrayList<>();
    // removed unused commented out code
    private static volatile String curr_peer_ip = null;
    MessageHandler msg = new MessageHandler();
    public void start_router() {
        new Thread(() -> {
            System.out.println("[ROUTER] Opening router socket");
            try (ServerSocket s = new ServerSocket(5401)) {
                s.setSoTimeout(20000);
                int error_counter = 0;
                while (error_counter < 4) {
                    try {
                        route(s);
                    } catch (SocketTimeoutException e2) {
                        if (curr_peer_ip != null) {
                            System.out.println("[ROUTER] No incoming requests, refreshing list");
                            try {
                                request_data(curr_peer_ip);
                            } catch (IOException ie) {
                                System.err.println("[ROUTER] Error while refreshing list " + ie);
                            }
                        } else {
                            System.out.println("[ROUTER] No ip to contact, in bootstrap mode.");
                        }
                    } catch (IOException e) {
                        System.err.println("[ROUTER] Router error! " + e);
                        ++error_counter;
                    }
                }
            } catch (IOException e) {
                System.out.println("[ROUTER] Could not bind to port! " + e);
            }
        }
        ).start();
    }

    public void route(ServerSocket s) throws IOException {
        MessageHandler msg = new MessageHandler();


            Socket c = s.accept();
            PrintWriter out = new PrintWriter(c.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
            System.out.println("[ROUTER] Verifying handshake; Router request from " + c.getInetAddress().getHostAddress());
            String handshake = msg.decode(in.readLine());
            if (Objects.equals(handshake, Protocol.router_header)) {
                out.println(msg.encode(Protocol.router_header));
                String response = in.readLine();
                if (Objects.equals(response, "OK")) {
                    System.out.println("[ROUTER] Sending " + c.getInetAddress().getHostAddress() + " known peers.");
                    out.println(msg.encode(Protocol.router_header));
                    out.println(msg.encode(String.valueOf(known_peers.size())));
                    for (String ip : known_peers) {
                        out.println(msg.encode(ip));
                        in.readLine();
                    }
                    out.println(msg.encode("NICK"));
                    for (String nick : peer_names) {
                        out.println(msg.encode(nick));
                        in.readLine();
                    }
                    out.println(msg.encode(Protocol.router_header));
                }
            } else {
                System.err.println("[ROUTER] Invalid handshake\nExpected: " + Protocol.router_header + "\nGot: " + handshake);
                c.close();
            }

    }
    public void request_data(String ip_address) throws IOException {
        Socket c = new Socket(ip_address, 5401);
        PrintWriter out = new PrintWriter(c.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        System.out.println("[ROUTER] Requesting data from peer");
        out = new PrintWriter(c.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        System.out.println("[ROUTER] Sending handshake to " + c.getInetAddress().getHostAddress());
        out.println(msg.encode(Protocol.router_header));
        String response = msg.decode(in.readLine());
        if (Objects.equals(response, Protocol.router_header)) {
            out.println("OK");
            System.out.println("[ROUTER] Handshake verified, receiving peers");
            String first_header = msg.decode(in.readLine());
            if (!Objects.equals(first_header, Protocol.router_header)) {
                System.err.println("[ROUTER] Unexpected behaviour from peer, disconnecting");
                c.close();
                return;
            }
            String size_string = msg.decode(in.readLine());
            int size = Integer.parseInt(size_string);
            int nick_size = size;
            while (size > 0) {
                String read_data = msg.decode(in.readLine());
                if (known_peers.contains(read_data)) {
                    --size;
                    out.println(msg.encode("OK"));
                    continue;
                }
                known_peers.add(read_data);
                out.println(msg.encode("OK"));
                --size;
            }
            String separator = msg.decode(in.readLine());
            if (!Objects.equals(separator, "NICK")) {
                System.err.println("[ROUTER] Unexpected behaviour from peer, disconnecting.");
                c.close();
                return;
            }
            while (nick_size > 0) {
                String read_data = msg.decode(in.readLine());
                peer_names.add(read_data);
                out.println(msg.encode("OK"));
                --nick_size;
            }
            String last_header = in.readLine();
            System.out.println("[ROUTER] Successfully updated list of peers, disconnecting from peer.");
            c.close();
        }
    }
    public void add_peer(String ip, String nick) {
        known_peers.add(ip);
        peer_names.add(nick);
    }
    @SuppressWarnings("unused")
    // to be used in a later version
    public void remove_peer(String ip) {
        int index = known_peers.indexOf(ip);
        known_peers.remove(index);
        peer_names.remove(index);
    }
    public void setCurr_peer_ip(String ip) {
        Router.curr_peer_ip = ip;
    }
    public String print_peers() throws IOException {
        if (!known_peers.isEmpty()) {
            int numcounter = 1;
            for (String ips : known_peers) {
                int index = known_peers.indexOf(ips);
                String nick = peer_names.get(index);
                System.out.println(numcounter + ". " + ips + " - " + nick);
                ++numcounter;
            }
            Scanner scan = new Scanner(System.in);
            System.out.print("Please select an IP: ");
            String selected = scan.nextLine();
            int sel = Integer.parseInt(selected);
            return known_peers.get(sel);
        } else {
            System.out.println("[ROUTER] No other peers, currently in bootstrap mode");
            return "no";
        }

    }
}
