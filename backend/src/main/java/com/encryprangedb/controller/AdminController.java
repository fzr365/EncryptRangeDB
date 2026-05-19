package com.encryprangedb.controller;

import com.encryprangedb.model.AuditLogResponse;
import com.encryprangedb.model.ExperimentStatsResponse;
import com.encryprangedb.model.OpePolicyRequest;
import com.encryprangedb.model.OpePolicyResponse;
import com.encryprangedb.model.RebuildEafsRequest;
import com.encryprangedb.model.RebuildEafsResponse;
import com.encryprangedb.service.AuditLogService;
import com.encryprangedb.service.EafsOrderedIndexService;
import com.encryprangedb.service.ExperimentStatsService;
import com.encryprangedb.service.KeyManagementService;
import com.encryprangedb.service.OpePolicyService;
import com.encryprangedb.service.RecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ExperimentStatsService experimentStatsService;
    private final OpePolicyService opePolicyService;
    private final AuditLogService auditLogService;
    private final EafsOrderedIndexService eafsOrderedIndexService;
    private final KeyManagementService keyManagementService;
    private final RecordService recordService;

    public AdminController(ExperimentStatsService experimentStatsService,
                           OpePolicyService opePolicyService,
                           AuditLogService auditLogService,
                           EafsOrderedIndexService eafsOrderedIndexService,
                           KeyManagementService keyManagementService,
                           RecordService recordService) {
        this.experimentStatsService = experimentStatsService;
        this.opePolicyService = opePolicyService;
        this.auditLogService = auditLogService;
        this.eafsOrderedIndexService = eafsOrderedIndexService;
        this.keyManagementService = keyManagementService;
        this.recordService = recordService;
    }

    @GetMapping("/stats")
    public ExperimentStatsResponse stats() {
        return experimentStatsService.summary();
    }

    @GetMapping("/ope-policy")
    public OpePolicyResponse activePolicy() {
        return opePolicyService.getActivePolicy();
    }

    @PostMapping("/ope-policy")
    public OpePolicyResponse savePolicy(@Valid @RequestBody OpePolicyRequest request) {
        long start = System.currentTimeMillis();
        OpePolicyResponse response = opePolicyService.saveActivePolicy(request);
        auditLogService.log("OPE_POLICY_UPDATE", null, null, null, null, null,
                request.segments().size(), System.currentTimeMillis() - start, "SUCCESS", "policy=" + request.policyName());
        return response;
    }

    @GetMapping("/audit-logs")
    public List<AuditLogResponse> auditLogs(@RequestParam(defaultValue = "20") int limit) {
        return auditLogService.latest(limit);
    }

    @PostMapping("/rebuild-eafs")
    public RebuildEafsResponse rebuildEafs(@RequestBody(required = false) RebuildEafsRequest request) {
        long start = System.currentTimeMillis();
        if (request == null || request.rebuildAll()) {
            List<String> buckets = eafsOrderedIndexService.rebuildAllBuckets();
            auditLogService.log("EAFS_REBUILD", null, null, null, null, null,
                    buckets.size(), System.currentTimeMillis() - start, "SUCCESS", "scope=all");
            return new RebuildEafsResponse(buckets.size(), buckets);
        }
        if (request.table() == null || request.table().isBlank() || request.column() == null || request.column().isBlank()) {
            throw new IllegalArgumentException("table and column are required when rebuildAll is false");
        }
        eafsOrderedIndexService.rebuildBucket(request.table(), request.column());
        String bucket = request.table() + ":" + request.column();
        auditLogService.log("EAFS_REBUILD", null, request.table(), request.column(), null, null,
                1, System.currentTimeMillis() - start, "SUCCESS", "bucket=" + bucket);
        return new RebuildEafsResponse(1, List.of(bucket));
    }

    @GetMapping("/keys")
    public KeyManagementService.KeyStatus keyStatus() {
        return keyManagementService.status();
    }

    @PostMapping("/keys/rotate-demo")
    public KeyManagementService.KeyStatus rotateDemoKeyVersion() {
        KeyManagementService.KeyStatus status = keyManagementService.rotateDemoVersion();
        auditLogService.log("KEY_ROTATE_DEMO", null, null, null, null, null,
                null, 0L, "SUCCESS", "activeVersion=" + status.activeVersion());
        return status;
    }

    @PostMapping("/repair-integrity-tags")
    public RecordService.IntegrityRepairResult repairIntegrityTags() {
        long start = System.currentTimeMillis();
        RecordService.IntegrityRepairResult result = recordService.repairIntegrityTags();
        auditLogService.log("INTEGRITY_REPAIR", null, null, null, null, null,
                result.repairedRecords(), System.currentTimeMillis() - start, "SUCCESS",
                "skipped=" + result.skippedRecords());
        return result;
    }

    @PostMapping("/repair-index-integrity-tags")
    public RecordService.IndexIntegrityRepairResult repairIndexIntegrityTags() {
        long start = System.currentTimeMillis();
        RecordService.IndexIntegrityRepairResult result = recordService.repairIndexIntegrityTags();
        auditLogService.log("INDEX_INTEGRITY_REPAIR", null, null, null, null, null,
                result.repairedIndexes(), System.currentTimeMillis() - start, "SUCCESS",
                "skipped=" + result.skippedIndexes());
        return result;
    }
}
