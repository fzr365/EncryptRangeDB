package com.encryprangedb.mapper;

import com.encryprangedb.model.entity.OpePolicyEntity;
import com.encryprangedb.model.entity.QueryAuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnalyticsMapper {
    Long countRecords();
    Long countIndexes();
    Long countChainNodes();
    Long countAuditLogs();
    Long avgRangeLatency();
    QueryAuditLogEntity latestRangeAudit();
    OpePolicyEntity activePolicy();
    List<QueryAuditLogEntity> listAuditLogs(@Param("limit") int limit);
    void insertAuditLog(QueryAuditLogEntity entity);
    void deactivatePolicies();
    void insertPolicy(OpePolicyEntity entity);
}
