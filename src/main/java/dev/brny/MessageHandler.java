package dev.brny;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;

import com.google.crypto.tink.*;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.hybrid.HybridDecryptFactory;
import com.google.crypto.tink.hybrid.HybridEncryptFactory;
import com.google.crypto.tink.hybrid.HybridKeyTemplates;
import com.google.crypto.tink.JsonKeysetWriter;

public class MessageHandler {
    private KeysetHandle privateKeysetHandle;
    private KeysetHandle publicKeysetHandle;
    private ArrayList<KeysetHandle> peer_keys = new ArrayList<>();
    private KeysetHandle curr_pubkey;

    public String cleanup_msg(String message_raw) {
        try {
            if (!message_raw.contains((Protocol.b64_header))) {
                return null;
            }
            String decoded_msg = decode(message_raw);
            int headerLen = Protocol.header.length();
            if (decoded_msg.length() < (headerLen * 2)) {
                return null;
            }
            String ciphertext = decoded_msg.substring(headerLen, decoded_msg.length() - headerLen);

            String result = decrypt(ciphertext);
            if (Objects.equals(result, "DEC_FAIL_ERR")) {
                return null;
            }
            return result;
        } catch (Exception e) {
            System.err.println("[WARN] Malformed packet received, disconnection advised");
            return null;
        }
    }
    public String wrap_msg(String message) {
        String encrypted_msg = encrypt(message);
        String wrapped = Protocol.header + encrypted_msg + Protocol.header;
        return encode(wrapped);
    }
    public  String router_cleanup_msg(String message) {
        int headerLen = Protocol.router_header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }
    public String router_wrap_msg(String message) {
        return Protocol.router_header + message + Protocol.router_header;
    }
    public String encode(String message) {
        byte[] bytes = message.getBytes(StandardCharsets.ISO_8859_1);
        String encoded = Base64.getEncoder().encodeToString(bytes);
        return Protocol.b64_header + encoded + Protocol.b64_header;
    }
    public String decode(String message_raw) {
        try {
            int headerLen = Protocol.b64_header.length();
            String message = message_raw.substring(headerLen, message_raw.length() - headerLen);
            byte[] decoded = Base64.getDecoder().decode(message);
            return new String(decoded, StandardCharsets.ISO_8859_1);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("[ERROR] Invalid handshake");
            return null;
        }
    }
    public void crypt_init() throws GeneralSecurityException {
        System.out.println("[OK] Generating encryption keypair");
        TinkConfig.register();
        privateKeysetHandle = KeysetHandle.generateNew(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256);
        publicKeysetHandle = privateKeysetHandle.getPublicKeysetHandle();
    }
    public String encrypt(String message) {
        try {
            HybridEncrypt hybridEncrypt = HybridEncryptFactory.getPrimitive(curr_pubkey);
            byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);
            byte[] contextInfo = Protocol.header.getBytes(StandardCharsets.UTF_8);

            byte[] ciphertext = hybridEncrypt.encrypt(plaintext, contextInfo);
            return new String(ciphertext, StandardCharsets.ISO_8859_1);
        } catch (GeneralSecurityException e) {
            System.err.println("[ERROR] Encryption failed " + e);
            return "ENC_FAIL_ERR";
        }
    }
    public String decrypt(String message_raw) {
        try {
            byte[] ciphertext = message_raw.getBytes(StandardCharsets.ISO_8859_1);
            HybridDecrypt hybridDecrypt = HybridDecryptFactory.getPrimitive(privateKeysetHandle);
            byte[] contextInfo = Protocol.header.getBytes(StandardCharsets.UTF_8);
            byte[] decrypted = hybridDecrypt.decrypt(ciphertext, contextInfo);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            System.err.println("[ERROR] Decryption failed " + e);
            return "DEC_FAIL_ERR";
        }
    }
    public String get_key() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withOutputStream(outputStream));
            return outputStream.toString();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to export public key " + e);
            return "ENC_FAIL_ERR";
        }
    }
    public void set_curr_key(String raw_key) throws GeneralSecurityException, IOException {
        String key = raw_key;
        try {
            curr_pubkey = CleartextKeysetHandle.read(JsonKeysetReader.withString(key));
        } catch (GeneralSecurityException e) {
            System.err.println("[ERROR] Error while decoding peer's key! " + e);
            throw e;
        } catch (IOException e) {
            System.err.println("[ERROR] Error while decoding peer's key! " + e);
            throw e;
        }
    }
}
