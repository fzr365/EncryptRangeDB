package com.encryprangedb.model.entity;

import java.time.OffsetDateTime;

public class EafsOrderedNodeEntity {
    private Long id;
    private String bucket;
    private Long chainOrder;
    private String recordId;
    private Long rindex;
    private Long prevNodeId;
    private Long nextNodeId;
    private String chainKeyHex;
    private String payloadBase64;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public Long getChainOrder() {
        return chainOrder;
    }

    public void setChainOrder(Long chainOrder) {
        this.chainOrder = chainOrder;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public Long getRindex() {
        return rindex;
    }

    public void setRindex(Long rindex) {
        this.rindex = rindex;
    }

    public Long getPrevNodeId() {
        return prevNodeId;
    }

    public void setPrevNodeId(Long prevNodeId) {
        this.prevNodeId = prevNodeId;
    }

    public Long getNextNodeId() {
        return nextNodeId;
    }

    public void setNextNodeId(Long nextNodeId) {
        this.nextNodeId = nextNodeId;
    }

    public String getChainKeyHex() {
        return chainKeyHex;
    }

    public void setChainKeyHex(String chainKeyHex) {
        this.chainKeyHex = chainKeyHex;
    }

    public String getPayloadBase64() {
        return payloadBase64;
    }

    public void setPayloadBase64(String payloadBase64) {
        this.payloadBase64 = payloadBase64;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
