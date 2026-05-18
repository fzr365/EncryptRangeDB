package com.encryprangedb.service;

import com.encryprangedb.auth.AuthContext;
import com.encryprangedb.mapper.AnalyticsMapper;
import com.encryprangedb.model.AuditLogResponse;
import com.encryprangedb.model.entity.QueryAuditLogEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AnalyticsMapper analyticsMapper;

    public AuditLogService(AnalyticsMapper analyticsMapper) {
        this.analyticsMapper = analyticsMapper;
    }

    public void log(String actionType, String sqlText, String tableName, String columnName,
                    Long lowerIndex, Long upperIndex, Integer hitCount, Long elapsedMs, String status, String detailText) {
        try {
            QueryAuditLogEntity entity = new QueryAuditLogEntity();
            entity.setActionType(actionType);
            entity.setSqlText(sqlText);
            entity.setTableName(tableName);
            entity.setColumnName(columnName);
            entity.setLowerIndex(lowerIndex);
            entity.setUpperIndex(upperIndex);
            entity.setHitCount(hitCount);
            entity.setElapsedMs(elapsedMs);
            entity.setStatus(status);
            String actor = "actor=" + AuthContext.usernameOrSystem();
            entity.setDetailText(detailText == null || detailText.isBlank() ? actor : actor + "; " + detailText);
            analyticsMapper.insertAuditLog(entity);
        } catch (Exception ex) {
            log.warn("Skip audit log write for actionType={}: {}", actionType, ex.getMessage());
        }
    }

    public List<AuditLogResponse> latest(int limit) {
        try {
            return analyticsMapper.listAuditLogs(Math.min(Math.max(limit, 1), 100)).stream()
                    .map(log -> new AuditLogResponse(
                            log.getId(), log.getActionType(), log.getSqlText(), log.getTableName(), log.getColumnName(),
                            log.getLowerIndex(), log.getUpperIndex(), log.getHitCount(), log.getElapsedMs(), log.getStatus(),
                            log.getDetailText(), log.getCreatedAt()))
                    .toList();
        } catch (Exception ex) {
            log.warn("Read audit logs failed: {}", ex.getMessage());
            return List.of();
        }
    }
}
