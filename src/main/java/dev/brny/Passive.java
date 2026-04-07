package dev.brny;

import java.net.*;
import java.io.*;

// This is the class for when the user is waiting for somebody to connect to them.

public class Passive {
    private ServerSocket s;
    private Socket c;
    private PrintWriter out;
    private BufferedReader in;

    public void start(int port) {
        try {
            s = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            c = s.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            out = new PrintWriter(c.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
