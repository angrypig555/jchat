package dev.brny;

public class MessageHandler {
    public String cleanup_msg(String message) {
        int headerLen = Protocol.header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }
    public String wrap_msg(String message) {
        return Protocol.header + message + Protocol.header;
    }
}
