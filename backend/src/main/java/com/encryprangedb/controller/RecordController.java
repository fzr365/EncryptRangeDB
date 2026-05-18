package com.encryprangedb.controller;

import com.encryprangedb.model.PlainInsertRequest;
import com.encryprangedb.model.DecryptAuditRequest;
import com.encryprangedb.model.EncryptedInsertRequest;
import com.encryprangedb.model.RangeQueryRequest;
import com.encryprangedb.model.RangeQueryResponse;
import com.encryprangedb.service.AuditLogService;
import com.encryprangedb.service.RecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;
    private final AuditLogService auditLogService;

    public RecordController(RecordService recordService, AuditLogService auditLogService) {
        this.recordService = recordService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/plain")
    public ResponseEntity<Object> insertPlain(@Valid @RequestBody PlainInsertRequest request) {
        long start = System.currentTimeMillis();
        var record = recordService.insertPlain(request);
        auditLogService.log("INSERT_PLAIN", null, request.table(), null, null, null,
                request.fields().size(), System.currentTimeMillis() - start, "SUCCESS", "recordId=" + request.recordId());
        return ResponseEntity.ok(record);
    }

    @PostMapping("/encrypted")
    public ResponseEntity<Object> insertEncrypted(@Valid @RequestBody EncryptedInsertRequest request) {
        long start = System.currentTimeMillis();
        var record = recordService.insertEncrypted(request);
        auditLogService.log("INSERT_ENCRYPTED", null, request.table(), null, null, null,
                request.fields().size(), System.currentTimeMillis() - start, "SUCCESS", "recordId=" + request.recordId());
        return ResponseEntity.ok(record);
    }

    @PostMapping("/range")
    public RangeQueryResponse range(@Valid @RequestBody RangeQueryRequest request) {
        long start = System.currentTimeMillis();
        var rows = recordService.queryRange(request);
        auditLogService.log("RANGE_QUERY", null, request.table(), request.column(), request.lowerIndex(), request.upperIndex(),
                rows.size(), System.currentTimeMillis() - start, "SUCCESS", "range query ok");
        return new RangeQueryResponse(rows);
    }

    @GetMapping("/latest")
    public RangeQueryResponse latest(@RequestParam(defaultValue = "employees") String table,
                                     @RequestParam(defaultValue = "salary") String column,
                                     @RequestParam(defaultValue = "20") int limit) {
        return new RangeQueryResponse(recordService.latest(table, column, limit));
    }

    @PostMapping("/decrypt-audit")
    public ResponseEntity<Object> decryptAudit(@RequestBody DecryptAuditRequest request) {
        auditLogService.log("DECRYPT_VIEW", null, request.table(), null, null, null,
                request.fieldCount(), 0L, "SUCCESS", "recordId=" + request.recordId());
        return ResponseEntity.ok().build();
    }
}
