package com.encryprangedb.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AESUtilTest {

    @Test
    void encryptThenDecrypt() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "salary".getBytes(StandardCharsets.UTF_8);
        byte[] plain = "8000".getBytes(StandardCharsets.UTF_8);
        SecureRandom random = new SecureRandom();

        AESUtil.AesGcmPayload payload = AESUtil.encrypt(key, plain, aad, random);
        byte[] recovered = AESUtil.decrypt(key, payload);
        assertArrayEquals(plain, recovered);
    }
}

