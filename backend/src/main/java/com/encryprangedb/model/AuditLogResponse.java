package com.encryprangedb.model;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        Long id,
        String actionType,
        String sqlText,
        String tableName,
        String columnName,
        Long lowerIndex,
        Long upperIndex,
        Integer hitCount,
        Long elapsedMs,
        String status,
        String detailText,
        OffsetDateTime createdAt
) {
}
