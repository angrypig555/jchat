package dev.brny;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MessageHandler {
    public String cleanup_msg(String message_raw) {
        String message = decode(message_raw);
        int headerLen = Protocol.header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }
    public String wrap_msg(String message) {
        return encode(Protocol.header + message + Protocol.header);
    }
    public  String router_cleanup_msg(String message) {
        int headerLen = Protocol.router_header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }
    public String router_wrap_msg(String message) {
        return Protocol.router_header + message + Protocol.router_header;
    }
    public String encode(String message) {
        String encoded = Base64.getEncoder().encodeToString(message.getBytes());
        return Protocol.b64_header + encoded + Protocol.b64_header;
    }
    public String decode(String message_raw) {
        int headerLen = Protocol.b64_header.length();
        String message =  message_raw.substring(headerLen, message_raw.length() - headerLen);
        byte[] decoded = Base64.getDecoder().decode(message);
        return new String(decoded);
    }
}
