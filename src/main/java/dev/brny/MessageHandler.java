package dev.brny;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Objects;

import com.google.crypto.tink.*;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.hybrid.HybridDecryptFactory;
import com.google.crypto.tink.hybrid.HybridEncryptFactory;
import com.google.crypto.tink.hybrid.HybridKeyTemplates;
import com.google.crypto.tink.JsonKeysetWriter;

import java.util.logging.Level;

public class MessageHandler {
    // define all the keys so we can use them later
    private KeysetHandle privateKeysetHandle;
    private KeysetHandle publicKeysetHandle;
    private KeysetHandle curr_pubkey;
    /*
    * Cleans up message
    *
    * */
    public String cleanup_msg(String message_raw) {
        try {
            // first we decode b64
            if (!message_raw.contains((Protocol.b64_header))) {
                Log.get().log(Level.WARNING, "Message did not contain header");
                return null;
            }
            // we remove the headers
            String decoded_msg = decode(message_raw);
            int headerLen = Protocol.header.length();
            if (decoded_msg.length() < (headerLen * 2)) {
                Log.get().log(Level.WARNING, "Message was not long enough to contain header after decoding");
                return null;
            }
            String ciphertext = decoded_msg.substring(headerLen, decoded_msg.length() - headerLen);
            // then we decrypt it
            String result = decrypt(ciphertext);
            if (Objects.equals(result, "DEC_FAIL_ERR")) {
                Log.get().log(Level.SEVERE, "Message could not be decoded!");
                return null;
                // this should usually not happen, instead it would throw an exception
            }
            return result;
        } catch (Exception e) {
            Log.get().log(Level.WARNING, "Bad packet received");
            System.err.println("[WARN] Bad packet received, there may be a man in the middle, disconnection is advised");
            // usually this never happens, if it happens tho something really went wrong, mainly a man can be in the middle
            return null;
        }
    }
    // wraps encrypts and encodes the message
    public String wrap_msg(String message) {
        String encrypted_msg = encrypt(message);
        // important to double wrap it so we know when it ends
        String wrapped = Protocol.header + encrypted_msg + Protocol.header;
        return encode(wrapped);
    }
    //unused
    @SuppressWarnings("unused")
    public  String router_cleanup_msg(String message) {
        int headerLen = Protocol.router_header.length();
        return message.substring(headerLen, message.length() - headerLen);
    }
    //unused
    @SuppressWarnings("unused")
    public String router_wrap_msg(String message) {
        return Protocol.router_header + message + Protocol.router_header;
    }
    // encodes, not to be directly used
    public String encode(String message) {
        byte[] bytes = message.getBytes(StandardCharsets.ISO_8859_1);
        String encoded = Base64.getEncoder().encodeToString(bytes);
        return Protocol.b64_header + encoded + Protocol.b64_header;
        // encodes in b64 so the message integrity is always good and we dont break tink by giving it a corrupted message
    }
    // decodes b64, not to be directly used
    public String decode(String message_raw) {
        try {
            int headerLen = Protocol.b64_header.length();
            String message = message_raw.substring(headerLen, message_raw.length() - headerLen);
            byte[] decoded = Base64.getDecoder().decode(message);
            return new String(decoded, StandardCharsets.ISO_8859_1);
        } catch (StringIndexOutOfBoundsException e) {
            Log.get().log(Level.SEVERE, "Invalid handshake " + e);
            System.err.println("[ERROR] Invalid handshake");
            return null;
            // usually this is because of a version mismatch, or just garbage data
        }
    }
    // initializes keys, only to be used once
    @SuppressWarnings("deprecation")
    public void crypt_init() throws GeneralSecurityException {
        System.out.println("[OK] Generating encryption keypair");
        Log.get().log(Level.INFO, "Generating keys");
        TinkConfig.register();
        privateKeysetHandle = KeysetHandle.generateNew(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_CTR_HMAC_SHA256);
        publicKeysetHandle = privateKeysetHandle.getPublicKeysetHandle();
        // this shouldnt really throw an error, unless tink is not feeling well
    }
    // encrypts message, not to be used standalone

    public String encrypt(String message) {
        try {
            @SuppressWarnings("deprecation")
            HybridEncrypt hybridEncrypt = HybridEncryptFactory.getPrimitive(curr_pubkey);
            byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);
            byte[] contextInfo = Protocol.header.getBytes(StandardCharsets.UTF_8);

            byte[] ciphertext = hybridEncrypt.encrypt(plaintext, contextInfo);
            return new String(ciphertext, StandardCharsets.ISO_8859_1);
        } catch (GeneralSecurityException e) {
            System.err.println("[ERROR] Encryption failed " + e);
            Log.get().log(Level.SEVERE, "Encryption failed " + e);
            return "ENC_FAIL_ERR";
            // same as exporting keys, if this fails its either a problem in the code or somehow the pubkey got corrupted in ram
        }
    }
    // decrypts text, not to be used standalone
    public String decrypt(String message_raw) {
        try {
            byte[] ciphertext = message_raw.getBytes(StandardCharsets.ISO_8859_1);
            @SuppressWarnings("deprecation")
            // will move over to the non deprecated keyfactory later
            HybridDecrypt hybridDecrypt = HybridDecryptFactory.getPrimitive(privateKeysetHandle);
            byte[] contextInfo = Protocol.header.getBytes(StandardCharsets.UTF_8);
            byte[] decrypted = hybridDecrypt.decrypt(ciphertext, contextInfo);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            System.err.println("[ERROR] Decryption failed " + e);
            Log.get().log(Level.SEVERE, "Decryption failed! " + e);
            return "DEC_FAIL_ERR";
        }
    }
    // exports public key
    // will move over to the non deprecated, keyfactory later
    @SuppressWarnings("deprecation")
    public String get_key() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withOutputStream(outputStream));
            return outputStream.toString();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to export public key " + e);
            Log.get().log(Level.SEVERE, "Failed to export public key " + e);
            // this should not really happen, if it happens tho then something went really really wrong with tink.
            return "ENC_FAIL_ERR";
        }
    }
    // sets the current public key of the peer
    public void set_curr_key(String key) throws GeneralSecurityException, IOException {
        try {
            curr_pubkey = CleartextKeysetHandle.read(JsonKeysetReader.withString(key));
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("[ERROR] Error while decoding peer's key! " + e);
            Log.get().log(Level.SEVERE, "Error while decoding peer's key " + e);
            // if this happens the client may not be a jchat client
            throw e;
        }
    }
}
