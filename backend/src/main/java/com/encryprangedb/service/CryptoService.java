package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.crypto.AESUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
public class CryptoService {

    private final byte[] aesKey;
    private final SecureRandom random = new SecureRandom();
    private final OpePolicyService opePolicyService;

    public CryptoService(CryptoProperties props, OpePolicyService opePolicyService) {
        String aesKeyStr = props.getCrypto().getAesMasterKey();
        String opeKeyStr = props.getCrypto().getOpeMasterKey();
        if (StringUtils.isBlank(aesKeyStr) || StringUtils.isBlank(opeKeyStr)) {
            throw new IllegalStateException("AES/OPE keys must not be blank");
        }
        byte[] aes = aesKeyStr.getBytes(StandardCharsets.UTF_8);
        if (aes.length != 16 && aes.length != 24 && aes.length != 32) {
            throw new IllegalStateException("AES key length must be 16/24/32 bytes");
        }
        this.aesKey = aes;
        this.opePolicyService = opePolicyService;
    }

    public AESUtil.AesGcmPayload encryptField(String column, String plaintext) {
        // 列名作为 AAD 参与认证，避免密文被换到别的列后还能通过校验。
        byte[] aad = column.getBytes(StandardCharsets.UTF_8);
        return AESUtil.encrypt(aesKey, plaintext.getBytes(StandardCharsets.UTF_8), aad, random);
    }

    public long computeIndex(long value) {
        // 范围查询只依赖这个保序索引，不直接比较明文。
        return opePolicyService.encrypt(value);
    }
}
