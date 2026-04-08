package dev.brny;

public class MessageHandler {
    public String cleanup_msg(String message) {
        int headerLen = Protocol.header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }
    public String wrap_msg(String message) {
        return Protocol.header + message + Protocol.header;
    }
    public  String router_cleanup_msg(String message) {
        int headerLen = Protocol.router_header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }
    public String router_wrap_msg(String message) {
        return Protocol.router_header + message + Protocol.router_header;
    }
}
