package com.encryprangedb.model.entity;

import java.time.OffsetDateTime;

public class EncryptedRecordEntity {
    private Long id;
    private String tableName;
    private String recordId;
    private String cipherBlob;
    private String integrityTag;
    private String keyVersion;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getCipherBlob() {
        return cipherBlob;
    }

    public void setCipherBlob(String cipherBlob) {
        this.cipherBlob = cipherBlob;
    }

    public String getIntegrityTag() {
        return integrityTag;
    }

    public void setIntegrityTag(String integrityTag) {
        this.integrityTag = integrityTag;
    }

    public String getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(String keyVersion) {
        this.keyVersion = keyVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
