package com.encryprangedb.model.entity;

public class EncryptedIndexEntity {
    private Long id;
    private String tableName;
    private String columnName;
    private String recordId;
    private Long rindex;
    private Long skindex;
    private Integer segmentId;

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

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
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

    public Long getSkindex() {
        return skindex;
    }

    public void setSkindex(Long skindex) {
        this.skindex = skindex;
    }

    public Integer getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Integer segmentId) {
        this.segmentId = segmentId;
    }
}

