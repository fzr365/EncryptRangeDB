package com.encryprangedb.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public final class AESUtil {

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int NONCE_LEN = 12;
    private static final int TAG_LEN_BITS = 128;

    private AESUtil() {
    }

    public static AesGcmPayload encrypt(byte[] key, byte[] plaintext, byte[] aad, SecureRandom random) {
        try {
            byte[] nonce = new byte[NONCE_LEN];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LEN_BITS, nonce));
            if (aad != null) {
                cipher.updateAAD(aad);
            }
            byte[] cipherBytes = cipher.doFinal(plaintext);
            return new AesGcmPayload(
                    Base64.getEncoder().encodeToString(cipherBytes),
                    Base64.getEncoder().encodeToString(nonce),
                    aad == null ? null : Base64.getEncoder().encodeToString(aad)
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        }
    }

    public static byte[] decrypt(byte[] key, AesGcmPayload payload) {
        try {
            byte[] nonce = Base64.getDecoder().decode(payload.nonceBase64());
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LEN_BITS, nonce));
            if (payload.associatedDataBase64() != null) {
                cipher.updateAAD(Base64.getDecoder().decode(payload.associatedDataBase64()));
            }
            byte[] cipherBytes = Base64.getDecoder().decode(payload.ciphertextBase64());
            return cipher.doFinal(cipherBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM decrypt failed", e);
        }
    }

    public record AesGcmPayload(String ciphertextBase64, String nonceBase64, String associatedDataBase64) {
    }
}

