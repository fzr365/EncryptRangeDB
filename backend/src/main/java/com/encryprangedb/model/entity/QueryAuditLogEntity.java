package com.encryprangedb.model.entity;

import java.time.OffsetDateTime;

public class QueryAuditLogEntity {
    private Long id;
    private String actionType;
    private String sqlText;
    private String tableName;
    private String columnName;
    private Long lowerIndex;
    private Long upperIndex;
    private Integer hitCount;
    private Long elapsedMs;
    private String status;
    private String detailText;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public Long getLowerIndex() { return lowerIndex; }
    public void setLowerIndex(Long lowerIndex) { this.lowerIndex = lowerIndex; }
    public Long getUpperIndex() { return upperIndex; }
    public void setUpperIndex(Long upperIndex) { this.upperIndex = upperIndex; }
    public Integer getHitCount() { return hitCount; }
    public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDetailText() { return detailText; }
    public void setDetailText(String detailText) { this.detailText = detailText; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
