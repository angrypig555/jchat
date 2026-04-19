package dev.brny;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
// Filehandler.java - JChat Filesharing handler

public class FileHandler {
    public void share(File file) throws IOException {
        HashCode main_hash = Files.asByteSource(file).hash(Hashing.sha256());
        System.out.println("[FILE] Hosting hash: " + main_hash.toString());
        Log.get().log(Level.INFO, "Hosting hash " + main_hash.toString());
        ServerSocket s = new ServerSocket(5402);
        while (true) {
            Socket c = s.accept();
            System.out.println("[FILE] New connection from: " + c.getInetAddress().getHostAddress());
            Log.get().log(Level.INFO, "Connection from " + c.getInetAddress().getHostAddress());
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(c.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(c.getInputStream()));
            String handshake = read_tostring(in);
            if (!Objects.equals(handshake, Protocol.header)) {
                Log.get().log(Level.WARNING, "Invalid handshake, expected " + Protocol.header + " got " + handshake + " Closing socket.");
                c.close();
                continue;
            }
            send_string(out, Protocol.header);
            HashCode requested_hash = HashCode.fromBytes(read(in));
            if (!requested_hash.equals(main_hash)) {
                send_string(out, "NOT_HERE");
                Log.get().log(Level.FINE, "Requested hash is not hosted here");
                c.close();
                continue;
            } else {
                send_string(out, "HERE");
                Log.get().log(Level.FINE, "Hash is hosted here");
            }
            Log.get().log(Level.FINE, "Sending 1MB chunks to peer");
            send_string(out, file.getName());
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                byte[] chunk;
                while (true) {
                    bis.mark(1024 * 1024 + 1024);
                    chunk = bis.readNBytes(1024 * 1024);
                    if (chunk.length == 0) break;
                    byte[] hash = hash(chunk);
                    send(out, hash);
                    String hashresp = read_tostring(in);
                    send(out, chunk);
                    String response = read_tostring(in);
                    if (!Objects.equals(response, "OK")) {
                        bis.reset();
                    }
                }
                send_string(out, "STOP");
            }
            System.out.println("[FILE] File shared successfully");
            Log.get().log(Level.INFO, "File shared succesfully, closing socket");
            c.close();
        }
    }
    public void send(DataOutputStream out, byte[] data) throws IOException {
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }
    public void send_string(DataOutputStream out, String data_raw) throws IOException {
        byte[] data = data_raw.getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }
    public String read_tostring(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.readFully(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }
    public byte[] read(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.readFully(buffer);
        return buffer;
    }
    public void get_data(Router router, String raw_hash, String path) throws IOException {
        HashCode hash_to_get = HashCode.fromString(raw_hash);
        for (String ip : router.known_peers) {
            try (Socket c = new Socket()) {
                Log.get().log(Level.INFO, "Attempting to connect to " + ip);
                SocketAddress addr = new InetSocketAddress(ip, 5402);
                c.connect(addr, 2000);
                c.setSoTimeout(5000);
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(c.getOutputStream()));
                DataInputStream in = new DataInputStream(new BufferedInputStream(c.getInputStream()));
                send_string(out, Protocol.header);
                String reply = read_tostring(in);
                if (!Objects.equals(reply, Protocol.header)) {
                    Log.get().log(Level.WARNING, "Invalid header, expected " + Protocol.header + " got " + reply + " closing socket");
                    c.close();
                }
                send(out, hash_to_get.asBytes());
                String reply_from_hash = read_tostring(in);
                if (!Objects.equals(reply_from_hash, "HERE")) {
                    Log.get().log(Level.FINE, "Peer not hosting file, closing socket");
                    c.close();
                }
                String filename = read_tostring(in);
                System.out.println("[FILE] Downloading " + filename);
                Log.get().log(Level.INFO, "Downloading file");
                Path basedir = Paths.get(path);
                Path finalFile = basedir.resolve(filename);
                try (FileOutputStream fos = new FileOutputStream(finalFile.toFile(), false); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    while (true) {
                        byte[] hash_raw = read(in);
                        if (hash_raw.length == 4) {
                            String signal = new String(hash_raw, StandardCharsets.UTF_8);
                            if ("STOP".equals(signal)) {
                                System.out.println("[FILE] Download finished");
                                Log.get().log(Level.INFO, "Download finished");
                                break;
                            }
                        }
                        if (hash_raw == null || hash_raw.length != 32) {
                            c.close();
                            Log.get().log(Level.SEVERE, "Received invalid hash, not long enough to be a hash");
                            throw new IOException("[FILE] Received invalid hash, length: " + (hash_raw == null ? 0 : hash_raw.length));
                        }
                        HashCode hash = HashCode.fromBytes(hash_raw);
                        send_string(out, "OK");
                        byte[] chunk = read(in);
                        HashCode hashed_chunk = Hashing.sha256().hashBytes(chunk);
                        if (!hash.equals(hashed_chunk)) {
                            c.close();
                            Log.get().log(Level.WARNING, "Received invalid chunk");
                            throw new IOException("[FILE] Received invalid chunk");
                        }
                        bos.write(chunk);
                        bos.flush();
                        send_string(out, "OK");

                    }
                    bos.flush();
                }
                break;
            } catch (SocketTimeoutException e) {
                System.err.println("[FILE] Peer " + ip + " timed out, removing from router");
                Log.get().log(Level.WARNING, "Peer " + ip + " timed out, removing from router");
                router.remove_peer(ip);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
    public byte[] hash(byte[] file_chunk) {
        return Hashing.sha256().hashBytes(file_chunk).asBytes();
    }
    public void start_share(File file) {
        Thread thread = new Thread(() -> {
            try {
                share(file);
            } catch (IOException e) {
                System.err.println("[FILE] Error: " + e);
                Log.get().log(Level.SEVERE, "Error: " + e);
            }
        }

        );
        thread.start();
    }
}
