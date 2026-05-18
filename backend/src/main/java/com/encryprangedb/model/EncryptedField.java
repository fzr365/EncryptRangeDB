package com.encryprangedb.model;

public record EncryptedField(
        String column,
        String ciphertextBase64,
        String nonceBase64,
        Long rindex,
        Long skindex,
        Integer segmentId
) {
}

