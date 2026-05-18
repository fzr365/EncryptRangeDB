package com.encryprangedb.service;

import com.encryprangedb.mapper.AnalyticsMapper;
import com.encryprangedb.model.ExperimentStatsResponse;
import org.springframework.stereotype.Service;

@Service
public class ExperimentStatsService {
    private final AnalyticsMapper analyticsMapper;
    private final OpePolicyService opePolicyService;

    public ExperimentStatsService(AnalyticsMapper analyticsMapper, OpePolicyService opePolicyService) {
        this.analyticsMapper = analyticsMapper;
        this.opePolicyService = opePolicyService;
    }

    public ExperimentStatsResponse summary() {
        var latestRange = safeLatestRange();
        var policy = opePolicyService.getActivePolicy();
        long span = latestRange == null || latestRange.getLowerIndex() == null || latestRange.getUpperIndex() == null
                ? 0L
                : latestRange.getUpperIndex() - latestRange.getLowerIndex();
        return new ExperimentStatsResponse(
                safeCount(() -> analyticsMapper.countRecords()),
                safeCount(() -> analyticsMapper.countIndexes()),
                safeCount(() -> analyticsMapper.countChainNodes()),
                safeCount(() -> analyticsMapper.countAuditLogs()),
                safeCount(() -> analyticsMapper.avgRangeLatency()),
                latestRange == null || latestRange.getHitCount() == null ? 0 : latestRange.getHitCount(),
                span,
                policy.policyName(),
                policy.sensitivity(),
                policy.segments().size()
        );
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private long safeCount(java.util.function.Supplier<Long> supplier) {
        try {
            return nvl(supplier.get());
        } catch (Exception ex) {
            return 0L;
        }
    }

    private com.encryprangedb.model.entity.QueryAuditLogEntity safeLatestRange() {
        try {
            return analyticsMapper.latestRangeAudit();
        } catch (Exception ex) {
            return null;
        }
    }
}
