package com.encryprangedb.controller;

import com.encryprangedb.model.RangeQueryRequest;
import com.encryprangedb.model.SqlImportResult;
import com.encryprangedb.service.AuditLogService;
import com.encryprangedb.service.SqlRewriteService;
import com.encryprangedb.service.SqlImportService;
import net.sf.jsqlparser.JSQLParserException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/sql")
public class SqlController {

    private final SqlRewriteService sqlRewriteService;
    private final SqlImportService sqlImportService;
    private final AuditLogService auditLogService;

    public SqlController(SqlRewriteService sqlRewriteService, SqlImportService sqlImportService, AuditLogService auditLogService) {
        this.sqlRewriteService = sqlRewriteService;
        this.sqlImportService = sqlImportService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/translate")
    public RangeQueryRequest translate(@RequestBody String sql) throws JSQLParserException {
        long start = System.currentTimeMillis();
        try {
            RangeQueryRequest request = sqlRewriteService.translateRange(sql);
            auditLogService.log("SQL_TRANSLATE", sql, request.table(), request.column(), request.lowerIndex(), request.upperIndex(),
                    null, System.currentTimeMillis() - start, "SUCCESS", "translate ok");
            return request;
        } catch (JSQLParserException | RuntimeException ex) {
            auditLogService.log("SQL_TRANSLATE", sql, null, null, null, null,
                    null, System.currentTimeMillis() - start, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/import")
    public SqlImportResult importSql(@RequestParam("file") MultipartFile file) throws IOException, JSQLParserException {
        long start = System.currentTimeMillis();
        String sql = new String(file.getBytes(), StandardCharsets.UTF_8);
        try {
            SqlImportResult result = sqlImportService.importSql(sql);
            auditLogService.log("SQL_IMPORT", null, null, null, null, null, result.insertedRows(),
                    System.currentTimeMillis() - start, "SUCCESS", "import ok");
            return result;
        } catch (JSQLParserException | RuntimeException ex) {
            auditLogService.log("SQL_IMPORT", null, null, null, null, null,
                    null, System.currentTimeMillis() - start, "FAILED", ex.getMessage());
            throw ex;
        }
    }
}
