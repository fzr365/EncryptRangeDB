package com.encryprangedb.model.entity;

public class EafsAnchorEntity {
    private Long id;
    private String bucket;
    private Long anchorOrder;
    private Long anchorRindex;
    private Long nodeId;

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

    public Long getAnchorOrder() {
        return anchorOrder;
    }

    public void setAnchorOrder(Long anchorOrder) {
        this.anchorOrder = anchorOrder;
    }

    public Long getAnchorRindex() {
        return anchorRindex;
    }

    public void setAnchorRindex(Long anchorRindex) {
        this.anchorRindex = anchorRindex;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }
}
