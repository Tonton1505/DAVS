package vn.edu.davs.backend.service;


/**
 * AES-256-GCM encryption and decryption service for connection credentials (BUS-DAT-01)
 * and external OAuth tokens (BUS-INT-02).
 */
public interface SecretCryptoService {


    /**
     * Encrypts plaintext using AES-256-GCM with a random 12-byte IV.
     */
    String encrypt(String plaintext);


    /**
     * Decrypts a Base64-encoded AES-256-GCM payload.
     */
    String decrypt(String ciphertextBase64);
}
