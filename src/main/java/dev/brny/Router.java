// The router runs on a separate thread, completely independently of the normal communication on port 5401
package dev.brny;

import java.net.*;
import java.io.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;

public class Router {
    ArrayList<String> known_peers = new ArrayList<>();
    ArrayList<String> peer_names = new ArrayList<>();
    // removed unused commented out code
    private static volatile String curr_peer_ip = null;
    MessageHandler msg = new MessageHandler();
    // lots of logs here, will implement a logging library later on
    public void start_router() {
        new Thread(() -> { // seperate thread so we dont block other functions
            System.out.println("[ROUTER] Opening router socket");
            Log.get().log(Level.INFO, "Opening router socket");
            try (ServerSocket s = new ServerSocket(5401)) {
                s.setSoTimeout(20000);
                int error_counter = 0; // counts the error, this part is prone to crashing
                while (error_counter < 4) {
                    try {
                        route(s);
                    } catch (SocketTimeoutException e2) {
                        if (curr_peer_ip != null) {
                            System.out.println("[ROUTER] No incoming requests, refreshing list");
                            Log.get().log(Level.INFO, "Refreshing router list");
                            try {
                                request_data(curr_peer_ip);
                            } catch (IOException ie) {
                                System.err.println("[ROUTER] Error while refreshing list " + ie);
                                Log.get().log(Level.SEVERE, "Error refreshing router list " + ie);
                            }
                        } else {
                            System.out.println("[ROUTER] No ip to contact, in bootstrap mode.");
                            Log.get().log(Level.INFO, "In bootstrap mode");
                        }
                    } catch (IOException e) {
                        System.err.println("[ROUTER] Router error! " + e);
                        Log.get().log(Level.SEVERE, "Router error! " + e);
                        ++error_counter;
                    }
                }
            } catch (IOException e) {
                System.out.println("[ROUTER] Could not bind to port! " + e);
                Log.get().log(Level.SEVERE, "Router failed to bind to port! " + e);
            }
        }
        ).start();
    }

    public void route(ServerSocket s) throws IOException {
        MessageHandler msg = new MessageHandler();


            Socket c = s.accept();
            PrintWriter out = new PrintWriter(c.getOutputStream(), true); // printwriter and bufferedreader for reading input, will move away later to a better method
            BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
            System.out.println("[ROUTER] Verifying handshake; Router request from " + c.getInetAddress().getHostAddress());
            Log.get().log(Level.INFO, "Verifying handshake, request from " + c.getInetAddress().getHostAddress());
            String handshake = msg.decode(in.readLine());
            if (Objects.equals(handshake, Protocol.router_header)) {
                out.println(msg.encode(Protocol.router_header));
                Log.get().log(Level.INFO, "Sending header " + Protocol.router_header);
                String response = in.readLine();
                if (Objects.equals(response, "OK")) {
                    System.out.println("[ROUTER] Sending " + c.getInetAddress().getHostAddress() + " known peers.");
                    Log.get().log(Level.INFO, "Handshake correct, sending known peers");
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
                Log.get().log(Level.SEVERE, "Invalid router handshake, Expected: " + Protocol.router_header + " Got: " + handshake);
                c.close();
            }
    
    }
    public void request_data(String ip_address) throws IOException {
        Socket c = new Socket(ip_address, 5401);
        PrintWriter out = new PrintWriter(c.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        System.out.println("[ROUTER] Requesting data from peer");
        Log.get().log(Level.INFO, "Router; Requesting data from peer");
        out = new PrintWriter(c.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        System.out.println("[ROUTER] Sending handshake to " + c.getInetAddress().getHostAddress());
        Log.get().log(Level.INFO, "Router; Sending handshake to " + c.getInetAddress().getHostAddress());
        out.println(msg.encode(Protocol.router_header));
        String response = msg.decode(in.readLine());
        if (Objects.equals(response, Protocol.router_header)) {
            out.println("OK");
            System.out.println("[ROUTER] Handshake verified, receiving peers");
            Log.get().log(Level.INFO, "Router; Handshake correct, receiving peers");
            String first_header = msg.decode(in.readLine());
            if (!Objects.equals(first_header, Protocol.router_header)) {
                System.err.println("[ROUTER] Unexpected behaviour from peer, disconnecting");
                Log.get().log(Level.WARNING, "Router; Unexpected behaviour from peer, disconnecting");
                c.close();
                return;
            }
            String size_string = msg.decode(in.readLine());
            int size = Integer.parseInt(size_string);
            int nick_size = size;
            Log.get().log(Level.INFO, "Router; Getting " + size + " peers");
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
            Log.get().log(Level.INFO, "Router; Received IP's, getting nicknames");
            String separator = msg.decode(in.readLine());
            if (!Objects.equals(separator, "NICK")) {
                System.err.println("[ROUTER] Unexpected behaviour from peer, disconnecting.");
                Log.get().log(Level.WARNING, "Unexpected behaviour from peer, disconnecting.");
                c.close();
                return;
            }
            while (nick_size > 0) {
                String read_data = msg.decode(in.readLine());
                peer_names.add(read_data);
                out.println(msg.encode("OK"));
                --nick_size;
            }
            Log.get().log(Level.INFO, "Router; Received peer nicknames, successfully updated list, disconnecting");
            String last_header = in.readLine();
            System.out.println("[ROUTER] Successfully updated list of peers, disconnecting from peer.");
            c.close();
        }
    }
    public void add_peer(String ip, String nick) {
        known_peers.add(ip);
        peer_names.add(nick);
        Log.get().log(Level.FINE, "Added peer " + ip + " " + nick);
    }
    @SuppressWarnings("unused")
    // to be used in a later version
    public void remove_peer(String ip) {
        int index = known_peers.indexOf(ip);
        known_peers.remove(index);
        peer_names.remove(index);
        Log.get().log(Level.FINE, "Removed peer " + ip);
    }
    public void setCurr_peer_ip(String ip) {
        Router.curr_peer_ip = ip;
        Log.get().log(Level.FINE, "Current peer ip " + ip);
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
