package com.encryprangedb.service;

import com.encryprangedb.mapper.RecordMapper;
import com.encryprangedb.model.EncryptedField;
import com.encryprangedb.model.EncryptedInsertRequest;
import com.encryprangedb.model.EncryptedRecord;
import com.encryprangedb.model.PlainInsertRequest;
import com.encryprangedb.model.RangeQueryRequest;
import com.encryprangedb.model.entity.EncryptedIndexEntity;
import com.encryprangedb.model.entity.EncryptedRecordEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecordService {

    private final RecordMapper recordMapper;
    private final CryptoService cryptoService;
    private final EafsOrderedIndexService eafsOrderedIndexService;
    private final IntegrityService integrityService;
    private final ObjectMapper objectMapper;

    public RecordService(RecordMapper recordMapper,
                         CryptoService cryptoService,
                         EafsOrderedIndexService eafsOrderedIndexService,
                         IntegrityService integrityService,
                         ObjectMapper objectMapper) {
        this.recordMapper = recordMapper;
        this.cryptoService = cryptoService;
        this.eafsOrderedIndexService = eafsOrderedIndexService;
        this.integrityService = integrityService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EncryptedRecord insertPlain(PlainInsertRequest request) {
        List<EncryptedField> fields = new ArrayList<>();
        for (PlainInsertRequest.Field field : request.fields()) {
            String column = field.column();
            String plaintext = String.valueOf(field.value());
            var payload = cryptoService.encryptField(column, plaintext);
            Long rindex = null;
            if (field.indexed()) {
                long numeric = parseLongStrict(field.value(), column);
                rindex = cryptoService.computeIndex(numeric);
            }
            fields.add(new EncryptedField(column, payload.ciphertextBase64(), payload.nonceBase64(), rindex, null, null));
        }

        EncryptedRecord record = new EncryptedRecord(request.table(), request.recordId(), fields);
        EncryptedRecordEntity entity = new EncryptedRecordEntity();
        entity.setTableName(record.table());
        entity.setRecordId(record.recordId());
        entity.setCipherBlob(serialize(record));
        entity.setKeyVersion(integrityService.currentKeyVersion());
        entity.setIntegrityTag(null);
        entity.setCreatedAt(OffsetDateTime.now());
        recordMapper.insertRecord(entity);
        refreshIntegrityTag(record.table(), record.recordId());

        for (EncryptedField field : fields) {
            if (field.rindex() == null) {
                continue;
            }
            insertIndexRow(record.table(), field.column(), record.recordId(), field.rindex(), null, null, entity.getKeyVersion());
            eafsOrderedIndexService.insertOrdered(record.table(), field.column(), field.rindex(), record.recordId());
        }
        return record;
    }

    @Transactional
    public EncryptedRecord insertEncrypted(EncryptedInsertRequest request) {
        EncryptedRecord record = new EncryptedRecord(request.table(), request.recordId(), request.fields());
        EncryptedRecordEntity entity = new EncryptedRecordEntity();
        entity.setTableName(record.table());
        entity.setRecordId(record.recordId());
        entity.setCipherBlob(serialize(record));
        entity.setKeyVersion(integrityService.currentKeyVersion());
        entity.setIntegrityTag(null);
        entity.setCreatedAt(OffsetDateTime.now());
        recordMapper.insertRecord(entity);
        refreshIntegrityTag(record.table(), record.recordId());

        for (EncryptedField field : record.fields()) {
            if (field.rindex() == null) {
                continue;
            }
            insertIndexRow(record.table(), field.column(), record.recordId(), field.rindex(), field.skindex(), field.segmentId(), entity.getKeyVersion());
            eafsOrderedIndexService.insertOrdered(record.table(), field.column(), field.rindex(), record.recordId());
        }
        return record;
    }

    public List<Map<String, Object>> queryRange(RangeQueryRequest req) {
        List<EafsOrderedIndexService.ChainHit> hits = eafsOrderedIndexService.scanRange(req.table(), req.column(), req.lowerIndex(), req.upperIndex());
        if (hits.isEmpty()) {
            return List.of();
        }
        List<String> orderedRecordIds = hits.stream().map(EafsOrderedIndexService.ChainHit::recordId).toList();
        List<Map<String, Object>> rows = new ArrayList<>(recordMapper.selectByRecordIds(req.table(), req.column(), orderedRecordIds));
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < orderedRecordIds.size(); i++) {
            orderMap.put(orderedRecordIds.get(i), i);
        }
        rows.sort(Comparator.comparingInt(row -> orderMap.getOrDefault(String.valueOf(row.get("record_id")), Integer.MAX_VALUE)));

        Map<String, Long> expectedRindex = hits.stream()
                .collect(Collectors.toMap(EafsOrderedIndexService.ChainHit::recordId, EafsOrderedIndexService.ChainHit::rindex, (left, right) -> left));
        verifyIndexRows(rows, req.table(), req.column(), expectedRindex);
        enrichRows(rows, req.table(), req.column());
        return rows;
    }

    public List<Map<String, Object>> latest(String table, String column, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<Map<String, Object>> rows = recordMapper.selectLatest(table, column, safeLimit);
        enrichRows(rows, table, column);
        return rows;
    }

    @Transactional
    public IntegrityRepairResult repairIntegrityTags() {
        int repaired = 0;
        int skipped = 0;
        for (EncryptedRecordEntity record : recordMapper.selectAllRecords()) {
            if (record.getTableName() == null || record.getRecordId() == null || record.getCipherBlob() == null) {
                skipped++;
                continue;
            }
            String keyVersion = normalizeKeyVersion(record.getKeyVersion());
            String tag = integrityService.tag(record.getTableName(), record.getRecordId(), record.getCipherBlob(), keyVersion);
            recordMapper.updateIntegrityTag(record.getId(), tag);
            repaired++;
        }
        return new IntegrityRepairResult(repaired, skipped);
    }

    @Transactional
    public IndexIntegrityRepairResult repairIndexIntegrityTags() {
        int repaired = 0;
        int skipped = 0;
        String fallbackKeyVersion = integrityService.currentKeyVersion();
        for (EncryptedIndexEntity idx : recordMapper.selectAllIndexes()) {
            if (idx.getId() == null
                    || idx.getTableName() == null
                    || idx.getColumnName() == null
                    || idx.getRecordId() == null
                    || idx.getRindex() == null) {
                skipped++;
                continue;
            }
            String keyVersion = normalizeKeyVersion(idx.getKeyVersion());
            if ("v1".equals(keyVersion) && (idx.getKeyVersion() == null || idx.getKeyVersion().isBlank())) {
                keyVersion = fallbackKeyVersion;
            }
            String tag = integrityService.tagIndex(
                    idx.getTableName(),
                    idx.getColumnName(),
                    idx.getRecordId(),
                    idx.getRindex(),
                    keyVersion);
            recordMapper.updateIndexIntegrity(idx.getId(), tag, keyVersion);
            repaired++;
        }
        return new IndexIntegrityRepairResult(repaired, skipped);
    }

    private void insertIndexRow(String table, String column, String recordId, long rindex, Long skindex, Integer segmentId, String keyVersion) {
        EncryptedIndexEntity idx = new EncryptedIndexEntity();
        idx.setTableName(table);
        idx.setColumnName(column);
        idx.setRecordId(recordId);
        idx.setRindex(rindex);
        idx.setSkindex(skindex);
        idx.setSegmentId(segmentId);
        idx.setKeyVersion(keyVersion);
        idx.setIndexTag(integrityService.tagIndex(table, column, recordId, rindex, keyVersion));
        recordMapper.insertIndex(idx);
    }

    private void refreshIntegrityTag(String table, String recordId) {
        EncryptedRecordEntity stored = recordMapper.selectRecord(table, recordId);
        if (stored == null || stored.getCipherBlob() == null) {
            throw new IllegalStateException("Inserted record cannot be reloaded for integrity tag");
        }
        String keyVersion = normalizeKeyVersion(stored.getKeyVersion());
        String tag = integrityService.tag(stored.getTableName(), stored.getRecordId(), stored.getCipherBlob(), keyVersion);
        recordMapper.updateIntegrityTag(stored.getId(), tag);
    }

    private void enrichRows(List<Map<String, Object>> rows, String table, String column) {
        if (rows.isEmpty()) {
            return;
        }
        List<String> recordIds = rows.stream()
                .map(row -> String.valueOf(row.get("record_id")))
                .collect(Collectors.toList());
        Map<String, EafsOrderedIndexService.ChainPreview> previewByRecordId =
                eafsOrderedIndexService.previewByRecordIds(table, column, recordIds);
        Map<String, Long> expectedRindex = previewByRecordId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().rindex()));
        verifyIndexRows(rows, table, column, expectedRindex);
        for (Map<String, Object> row : rows) {
            verifyIntegrity(row, table);
            String recordId = String.valueOf(row.get("record_id"));
            EafsOrderedIndexService.ChainPreview preview = previewByRecordId.get(recordId);
            if (preview != null) {
                row.put("chainCounter", preview.chainOrder());
                row.put("chainKeyHex", preview.chainKeyHex());
                row.put("prevRecordId", preview.prevRecordId());
                row.put("nextRecordId", preview.nextRecordId());
                row.put("rindex", preview.rindex());
            } else {
                row.put("prevRecordId", null);
                row.put("nextRecordId", null);
            }
        }
    }

    private void verifyIndexRows(List<Map<String, Object>> rows, String table, String column, Map<String, Long> expectedRindex) {
        for (Map<String, Object> row : rows) {
            String recordId = String.valueOf(row.get("record_id"));
            Long expected = expectedRindex.get(recordId);
            if (expected == null) {
                continue;
            }
            Long actual = asLong(row.get("rindex"));
            if (!Objects.equals(actual, expected)) {
                throw new IllegalStateException("Encrypted index mismatch detected, recordId=" + recordId);
            }
            String keyVersion = normalizeKeyVersion(row.get("key_version") == null ? null : String.valueOf(row.get("key_version")));
            String tag = row.get("index_tag") == null ? null : String.valueOf(row.get("index_tag"));
            if (!integrityService.verifyIndex(table, column, recordId, expected, keyVersion, tag)) {
                throw new IllegalStateException("Encrypted index integrity check failed, recordId=" + recordId);
            }
            row.put("indexIntegrityStatus", "PASS");
            row.put("indexKeyVersion", keyVersion);
        }
    }

    private void verifyIntegrity(Map<String, Object> row, String table) {
        String recordId = String.valueOf(row.get("record_id"));
        String cipherBlob = String.valueOf(row.get("cipher_blob"));
        Object tagObj = row.get("integrity_tag");
        Object keyVersionObj = row.get("key_version");
        String tag = tagObj == null ? null : String.valueOf(tagObj);
        String keyVersion = normalizeKeyVersion(keyVersionObj == null ? null : String.valueOf(keyVersionObj));
        if (tag == null || tag.isBlank()) {
            row.put("integrityStatus", "LEGACY_MISSING");
            return;
        }
        boolean ok = integrityService.verify(table, recordId, cipherBlob, keyVersion, tag);
        row.put("integrityStatus", ok ? "PASS" : "FAILED");
        row.put("keyVersion", keyVersion);
    }

    private long parseLongStrict(Object value, String column) {
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Indexed field must be integer-like, column=" + column);
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeKeyVersion(String keyVersion) {
        return keyVersion == null || keyVersion.isBlank() ? "v1" : keyVersion;
    }

    private String serialize(EncryptedRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serialize cipher blob failed", e);
        }
    }

    public record IntegrityRepairResult(int repairedRecords, int skippedRecords) {
    }

    public record IndexIntegrityRepairResult(int repairedIndexes, int skippedIndexes) {
    }
}
