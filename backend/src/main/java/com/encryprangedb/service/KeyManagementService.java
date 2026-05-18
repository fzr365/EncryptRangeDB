package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.crypto.HashUtil;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class KeyManagementService {
    private final CryptoProperties properties;
    private String runtimeVersion;
    private OffsetDateTime rotatedAt;

    public KeyManagementService(CryptoProperties properties) {
        this.properties = properties;
        this.runtimeVersion = properties.getCrypto().getKeyVersion();
        this.rotatedAt = OffsetDateTime.now();
    }

    public synchronized KeyStatus status() {
        return new KeyStatus(
                runtimeVersion,
                fingerprint(properties.getCrypto().getAesMasterKey()),
                fingerprint(properties.getCrypto().getOpeMasterKey()),
                fingerprint(properties.getCrypto().getIntegrityMasterKey()),
                keySource("ENCRYP_RANGE_AES_KEY"),
                keySource("ENCRYP_RANGE_OPE_KEY"),
                keySource("ENCRYP_RANGE_INTEGRITY_KEY"),
                rotatedAt
        );
    }

    public synchronized KeyStatus rotateDemoVersion() {
        String base = runtimeVersion == null || runtimeVersion.isBlank() ? "v1" : runtimeVersion;
        String numeric = base.toLowerCase(Locale.ROOT).startsWith("v") ? base.substring(1) : base;
        int next;
        try {
            next = Integer.parseInt(numeric) + 1;
        } catch (NumberFormatException ex) {
            next = 2;
        }
        runtimeVersion = "v" + next;
        rotatedAt = OffsetDateTime.now();
        return status();
    }

    public String currentVersion() {
        return runtimeVersion;
    }

    private String fingerprint(String key) {
        if (key == null || key.isBlank()) {
            return "未配置";
        }
        return Hex.encodeHexString(HashUtil.sha256(key.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
    }

    private String keySource(String envName) {
        return System.getenv(envName) == null ? "系统配置项" : "环境配置项";
    }

    public record KeyStatus(
            String activeVersion,
            String aesFingerprint,
            String opeFingerprint,
            String integrityFingerprint,
            String aesSource,
            String opeSource,
            String integritySource,
            OffsetDateTime rotatedAt
    ) {
    }
}
