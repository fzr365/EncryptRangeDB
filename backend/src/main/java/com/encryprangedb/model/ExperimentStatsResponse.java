package com.encryprangedb.model;

public record ExperimentStatsResponse(
        long totalRecords,
        long totalIndexedRows,
        long totalChainNodes,
        long totalAuditLogs,
        long avgQueryLatencyMs,
        long latestQueryHitCount,
        long latestRangeSpan,
        String activePolicyName,
        int activePolicySensitivity,
        int activePolicySegments
) {
}
