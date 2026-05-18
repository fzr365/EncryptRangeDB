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
        // 把表名、记录号、密钥版本和密文一起做 HMAC。
        String message = table + "|" + recordId + "|" + keyVersion + "|" + cipherBlob;
        return Hex.encodeHexString(HmacUtil.hmacSha256(integrityKey, message.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean verify(String table, String recordId, String cipherBlob, String keyVersion, String expectedTag) {
        // 查询结果返回前做一次校验，前端能看到 PASS 或 FAILED。
        if (expectedTag == null || expectedTag.isBlank()) {
            return false;
        }
        return tag(table, recordId, cipherBlob, keyVersion).equalsIgnoreCase(expectedTag);
    }
}
