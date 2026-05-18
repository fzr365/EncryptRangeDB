package com.encryprangedb.model;

import java.util.List;

public record EncryptedRecord(
        String table,
        String recordId,
        List<EncryptedField> fields
) {
}

