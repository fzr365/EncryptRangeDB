package com.encryprangedb.crypto;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public final class HashUtil {

    private HashUtil() {
    }

    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 failed", e);
        }
    }
}

