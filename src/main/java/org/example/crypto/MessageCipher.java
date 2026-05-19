package org.example.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

public class MessageCipher {
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public MessageCipher(SecretKey key) {
        this.key = key;
    }

    public static SecretKey keyFrom(byte[] rawKey) {
        return new SecretKeySpec(rawKey, "AES");
    }

    public byte[] encrypt(byte[] plaintext) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext);
        byte[] result = new byte[IV_SIZE + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, IV_SIZE);
        System.arraycopy(ciphertext, 0, result, IV_SIZE, ciphertext.length);
        return result;
    }

    public byte[] decrypt(byte[] data) throws Exception {
        if (data.length < IV_SIZE) {
            throw new IllegalArgumentException("Encrypted data too short to contain IV");
        }
        byte[] iv = Arrays.copyOfRange(data, 0, IV_SIZE);
        byte[] ciphertext = Arrays.copyOfRange(data, IV_SIZE, data.length);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }
}
