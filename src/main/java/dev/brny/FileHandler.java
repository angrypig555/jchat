package dev.brny;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

// Filehandler.java - JChat Filesharing handler

public class FileHandler {
    private ArrayList<String> hashes = new ArrayList<>();
    public void share() throws IOException {
        ServerSocket s = new ServerSocket(5402);
        Socket c = s.accept();
        System.out.println("[FILE] New connection from: " + c.getInetAddress().getHostAddress());
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(c.getOutputStream()));
        DataInputStream in = new DataInputStream(new BufferedInputStream(c.getInputStream()));
        String handshake = read(in);
        if (!Objects.equals(handshake, Protocol.header)) {
            c.close();
        }

    }
    public void send(DataOutputStream out, String data_raw) throws IOException {
        byte[] data = data_raw.getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }
    public String read(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.readFully(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }
    public void get_data() {

    }
}
