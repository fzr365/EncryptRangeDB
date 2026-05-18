package com.encryprangedb.model;

public record DecryptAuditRequest(
        String table,
        String recordId,
        Integer fieldCount
) {
}
