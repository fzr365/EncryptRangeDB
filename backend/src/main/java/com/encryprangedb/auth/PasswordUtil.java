package com.encryprangedb.auth;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public final class PasswordUtil {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;

    private PasswordUtil() {
    }

    public static String newSaltHex() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Hex.encodeHexString(salt);
    }

    public static String hash(String password, String saltHex) {
        try {
            byte[] salt = Hex.decodeHex(saltHex);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Hex.encodeHexString(encoded);
        } catch (GeneralSecurityException | DecoderException | IllegalArgumentException e) {
            throw new IllegalStateException("Password hash failed", e);
        }
    }

    public static boolean matches(String password, String saltHex, String expectedHash) {
        return hash(password, saltHex).equalsIgnoreCase(expectedHash);
    }
}
