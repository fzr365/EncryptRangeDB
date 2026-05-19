package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.crypto.HmacUtil;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class IntegrityService {
    private final byte[] integrityKey;
    private final KeyManagementService keyManagementService;

    public IntegrityService(CryptoProperties properties, KeyManagementService keyManagementService) {
        String key = properties.getCrypto().getIntegrityMasterKey();
        if (key == null || key.isBlank()) {
            key = properties.getCrypto().getAesMasterKey();
        }
        this.integrityKey = key.getBytes(StandardCharsets.UTF_8);
        this.keyManagementService = keyManagementService;
    }

    public String currentKeyVersion() {
        return keyManagementService.currentVersion();
    }

    public String tag(String table, String recordId, String cipherBlob, String keyVersion) {
        String message = table + "|" + recordId + "|" + keyVersion + "|" + cipherBlob;
        return Hex.encodeHexString(HmacUtil.hmacSha256(integrityKey, message.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean verify(String table, String recordId, String cipherBlob, String keyVersion, String expectedTag) {
        if (expectedTag == null || expectedTag.isBlank()) {
            return false;
        }
        return tag(table, recordId, cipherBlob, keyVersion).equalsIgnoreCase(expectedTag);
    }

    public String tagIndex(String table, String column, String recordId, long rindex, String keyVersion) {
        String message = table + "|" + column + "|" + recordId + "|" + rindex + "|" + keyVersion;
        return Hex.encodeHexString(HmacUtil.hmacSha256(integrityKey, message.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean verifyIndex(String table, String column, String recordId, long rindex, String keyVersion, String expectedTag) {
        if (expectedTag == null || expectedTag.isBlank()) {
            return false;
        }
        return tagIndex(table, column, recordId, rindex, keyVersion).equalsIgnoreCase(expectedTag);
    }
}
