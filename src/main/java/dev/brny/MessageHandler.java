package dev.brny;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;

import com.google.crypto.tink.*;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.hybrid.HybridDecryptFactory;
import com.google.crypto.tink.hybrid.HybridEncryptFactory;
import com.google.crypto.tink.hybrid.HybridKeyTemplates;

public class MessageHandler {
    private KeysetHandle privateKeysetHandle;
    private KeysetHandle publicKeysetHandle;
    private ArrayList<KeysetHandle> peer_keys = new ArrayList<>();
    private KeysetHandle curr_pubkey;

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
    public void crypt_init() throws GeneralSecurityException {
        System.out.println("[OK] Generating encryption keypair");
        TinkConfig.register();
        privateKeysetHandle = KeysetHandle.generateNew(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256);
        publicKeysetHandle = privateKeysetHandle.getPublicKeysetHandle();
    }
    public byte[] encrypt(String message) throws GeneralSecurityException {
        try {
            HybridEncrypt hybridEncrypt = HybridEncryptFactory.getPrimitive(curr_pubkey);
            return hybridEncrypt.encrypt(message.getBytes(), Protocol.header.getBytes());
        } catch (GeneralSecurityException e) {
            throw e;
        }
    }
    public String decrypt(byte[] message) throws GeneralSecurityException {
        try {
            HybridDecrypt hybridDecrypt = HybridDecryptFactory.getPrimitive(privateKeysetHandle);
            return new String(hybridDecrypt.decrypt(message, Protocol.header.getBytes()));
        } catch (GeneralSecurityException e) {
            throw e;
        }
    }
}
