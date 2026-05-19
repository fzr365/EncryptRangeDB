package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.mapper.AnalyticsMapper;
import com.encryprangedb.mapper.RecordMapper;
import com.encryprangedb.model.entity.OpePolicyEntity;
import com.encryprangedb.model.PlainInsertRequest;
import com.encryprangedb.model.RangeQueryRequest;
import com.encryprangedb.model.entity.EncryptedIndexEntity;
import com.encryprangedb.model.entity.EncryptedRecordEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordServiceTest {

    @Test
    void insertPlainShouldWriteRecordAndIndex() {
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        stubReloadInsertedRecord(recordMapper);
        EafsOrderedIndexService orderedIndexService = Mockito.mock(EafsOrderedIndexService.class);

        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto c = new CryptoProperties.Crypto();
        c.setAesMasterKey("0123456789abcdef0123456789abcdef");
        c.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        c.setSensitivity(1);
        props.setCrypto(c);

        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        OpePolicyEntity active = new OpePolicyEntity();
        active.setId(1L);
        active.setPolicyName("test-policy");
        active.setSensitivity(1);
        active.setActiveFlag(true);
        active.setSegmentJson("""
                [
                  {"minValue":0,"a":3,"b":10,"label":"low-range"},
                  {"minValue":100,"a":5,"b":20,"label":"mid-range"},
                  {"minValue":500,"a":10,"b":30,"label":"high-range"}
                ]
                """);
        when(analyticsMapper.activePolicy()).thenReturn(active);

        OpePolicyService opePolicyService = new OpePolicyService(analyticsMapper, props, new ObjectMapper());
        CryptoService cryptoService = new CryptoService(props, opePolicyService);
        RecordService recordService = new RecordService(recordMapper, cryptoService, orderedIndexService, integrityService(), new ObjectMapper());

        PlainInsertRequest req = new PlainInsertRequest(
                "employees",
                "emp-1001",
                List.of(
                        new PlainInsertRequest.Field("name", "Alice", false),
                        new PlainInsertRequest.Field("salary", 8000, true)
                )
        );

        recordService.insertPlain(req);

        verify(recordMapper).insertRecord(any());
        verify(recordMapper).updateIntegrityTag(Mockito.anyLong(), Mockito.anyString());
        verify(recordMapper).insertIndex(any());
    }

    @Test
    void insertPlainWithoutIndexedFieldShouldOnlyWriteRecord() {
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        stubReloadInsertedRecord(recordMapper);
        EafsOrderedIndexService orderedIndexService = Mockito.mock(EafsOrderedIndexService.class);

        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto c = new CryptoProperties.Crypto();
        c.setAesMasterKey("0123456789abcdef0123456789abcdef");
        c.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        c.setSensitivity(1);
        props.setCrypto(c);

        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        when(analyticsMapper.activePolicy()).thenReturn(null);

        OpePolicyService opePolicyService = new OpePolicyService(analyticsMapper, props, new ObjectMapper());
        CryptoService cryptoService = new CryptoService(props, opePolicyService);
        RecordService recordService = new RecordService(recordMapper, cryptoService, orderedIndexService, integrityService(), new ObjectMapper());

        PlainInsertRequest req = new PlainInsertRequest(
                "employees",
                "emp-1002",
                List.of(new PlainInsertRequest.Field("name", "Bob", false))
        );

        recordService.insertPlain(req);

        verify(recordMapper).insertRecord(any());
        verify(recordMapper).updateIntegrityTag(Mockito.anyLong(), Mockito.anyString());
        Mockito.verify(recordMapper, Mockito.never()).insertIndex(any());
    }

    @Test
    void latestShouldUseOrderedChainPreviewForEnrichment() {
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        EafsOrderedIndexService orderedIndexService = Mockito.mock(EafsOrderedIndexService.class);

        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto c = new CryptoProperties.Crypto();
        c.setAesMasterKey("0123456789abcdef0123456789abcdef");
        c.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        c.setSensitivity(1);
        props.setCrypto(c);

        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        when(analyticsMapper.activePolicy()).thenReturn(null);

        OpePolicyService opePolicyService = new OpePolicyService(analyticsMapper, props, new ObjectMapper());
        CryptoService cryptoService = new CryptoService(props, opePolicyService);
        RecordService recordService = new RecordService(recordMapper, cryptoService, orderedIndexService, integrityService(), new ObjectMapper());

        Map<String, Object> rowEmp2 = new HashMap<>();
        rowEmp2.put("record_id", "emp-2");
        rowEmp2.put("rindex", 200L);
        rowEmp2.put("index_tag", "tag-2");
        rowEmp2.put("key_version", "v1");
        Map<String, Object> rowEmp1 = new HashMap<>();
        rowEmp1.put("record_id", "emp-1");
        rowEmp1.put("rindex", 100L);
        rowEmp1.put("index_tag", "tag-1");
        rowEmp1.put("key_version", "v1");
        when(recordMapper.selectLatest("employees", "salary", 20)).thenReturn(List.of(rowEmp2, rowEmp1));

        when(orderedIndexService.previewByRecordIds("employees", "salary", List.of("emp-2", "emp-1")))
                .thenReturn(Map.of(
                        "emp-1", new EafsOrderedIndexService.ChainPreview(1L, "key-1", null, "emp-2", 100L),
                        "emp-2", new EafsOrderedIndexService.ChainPreview(2L, "key-2", "emp-1", null, 200L)
                ));

        List<Map<String, Object>> rows = recordService.latest("employees", "salary", 20);

        assertEquals(2L, rows.get(0).get("chainCounter"));
        assertEquals("emp-1", rows.get(0).get("prevRecordId"));
        assertEquals(null, rows.get(0).get("nextRecordId"));
        assertEquals(1L, rows.get(1).get("chainCounter"));
        assertEquals(null, rows.get(1).get("prevRecordId"));
        assertEquals("emp-2", rows.get(1).get("nextRecordId"));
    }

    @Test
    void queryRangeShouldRespectOrderedChainHitOrder() {
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        EafsOrderedIndexService orderedIndexService = Mockito.mock(EafsOrderedIndexService.class);

        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto c = new CryptoProperties.Crypto();
        c.setAesMasterKey("0123456789abcdef0123456789abcdef");
        c.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        c.setSensitivity(1);
        props.setCrypto(c);

        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        when(analyticsMapper.activePolicy()).thenReturn(null);

        OpePolicyService opePolicyService = new OpePolicyService(analyticsMapper, props, new ObjectMapper());
        CryptoService cryptoService = new CryptoService(props, opePolicyService);
        RecordService recordService = new RecordService(recordMapper, cryptoService, orderedIndexService, integrityService(), new ObjectMapper());

        when(orderedIndexService.scanRange("employees", "salary", 100L, 300L))
                .thenReturn(List.of(
                        new EafsOrderedIndexService.ChainHit("emp-2", 200L, 2L, "key-2", 1L, null),
                        new EafsOrderedIndexService.ChainHit("emp-1", 100L, 1L, "key-1", null, 2L)
                ));

        Map<String, Object> rowEmp1 = new HashMap<>();
        rowEmp1.put("record_id", "emp-1");
        rowEmp1.put("rindex", 100L);
        rowEmp1.put("index_tag", "tag-1");
        rowEmp1.put("key_version", "v1");
        Map<String, Object> rowEmp2 = new HashMap<>();
        rowEmp2.put("record_id", "emp-2");
        rowEmp2.put("rindex", 200L);
        rowEmp2.put("index_tag", "tag-2");
        rowEmp2.put("key_version", "v1");
        when(recordMapper.selectByRecordIds("employees", "salary", List.of("emp-2", "emp-1")))
                .thenReturn(List.of(rowEmp1, rowEmp2));
        when(orderedIndexService.previewByRecordIds("employees", "salary", List.of("emp-2", "emp-1")))
                .thenReturn(Map.of(
                        "emp-1", new EafsOrderedIndexService.ChainPreview(1L, "key-1", null, "emp-2", 100L),
                        "emp-2", new EafsOrderedIndexService.ChainPreview(2L, "key-2", "emp-1", null, 200L)
                ));

        List<Map<String, Object>> rows = recordService.queryRange(new RangeQueryRequest("employees", "salary", 100L, 300L));

        assertEquals("emp-2", rows.get(0).get("record_id"));
        assertEquals("emp-1", rows.get(1).get("record_id"));
    }

    @Test
    void queryRangeShouldRejectTamperedIndexTag() {
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        EafsOrderedIndexService orderedIndexService = Mockito.mock(EafsOrderedIndexService.class);

        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto c = new CryptoProperties.Crypto();
        c.setAesMasterKey("0123456789abcdef0123456789abcdef");
        c.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        c.setSensitivity(1);
        props.setCrypto(c);

        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        when(analyticsMapper.activePolicy()).thenReturn(null);

        OpePolicyService opePolicyService = new OpePolicyService(analyticsMapper, props, new ObjectMapper());
        CryptoService cryptoService = new CryptoService(props, opePolicyService);
        IntegrityService integrityService = integrityService();
        when(integrityService.verifyIndex("employees", "salary", "emp-1", 100L, "v1", "bad-tag")).thenReturn(false);
        RecordService recordService = new RecordService(recordMapper, cryptoService, orderedIndexService, integrityService, new ObjectMapper());

        when(orderedIndexService.scanRange("employees", "salary", 100L, 100L))
                .thenReturn(List.of(new EafsOrderedIndexService.ChainHit("emp-1", 100L, 1L, "key-1", null, null)));

        Map<String, Object> rowEmp1 = new HashMap<>();
        rowEmp1.put("record_id", "emp-1");
        rowEmp1.put("rindex", 100L);
        rowEmp1.put("index_tag", "bad-tag");
        rowEmp1.put("key_version", "v1");
        when(recordMapper.selectByRecordIds("employees", "salary", List.of("emp-1"))).thenReturn(List.of(rowEmp1));

        assertThrows(IllegalStateException.class,
                () -> recordService.queryRange(new RangeQueryRequest("employees", "salary", 100L, 100L)));
    }

    @Test
    void repairIndexIntegrityTagsShouldBackfillLegacyIndexes() {
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        EafsOrderedIndexService orderedIndexService = Mockito.mock(EafsOrderedIndexService.class);

        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto c = new CryptoProperties.Crypto();
        c.setAesMasterKey("0123456789abcdef0123456789abcdef");
        c.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        c.setSensitivity(1);
        props.setCrypto(c);

        AnalyticsMapper analyticsMapper = Mockito.mock(AnalyticsMapper.class);
        when(analyticsMapper.activePolicy()).thenReturn(null);

        OpePolicyService opePolicyService = new OpePolicyService(analyticsMapper, props, new ObjectMapper());
        CryptoService cryptoService = new CryptoService(props, opePolicyService);
        IntegrityService integrityService = integrityService();
        RecordService recordService = new RecordService(recordMapper, cryptoService, orderedIndexService, integrityService, new ObjectMapper());

        EncryptedIndexEntity idx = new EncryptedIndexEntity();
        idx.setId(1L);
        idx.setTableName("employees");
        idx.setColumnName("salary");
        idx.setRecordId("emp-1");
        idx.setRindex(100L);
        idx.setKeyVersion(null);
        when(recordMapper.selectAllIndexes()).thenReturn(List.of(idx));

        RecordService.IndexIntegrityRepairResult result = recordService.repairIndexIntegrityTags();

        assertEquals(1, result.repairedIndexes());
        assertEquals(0, result.skippedIndexes());
        verify(recordMapper).updateIndexIntegrity(1L, "index-tag", "v1");
    }

    private IntegrityService integrityService() {
        IntegrityService integrityService = Mockito.mock(IntegrityService.class);
        when(integrityService.currentKeyVersion()).thenReturn("v1");
        when(integrityService.tag(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn("test-tag");
        when(integrityService.verify(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        when(integrityService.tagIndex(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()))
                .thenReturn("index-tag");
        when(integrityService.verifyIndex(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);
        return integrityService;
    }

    private void stubReloadInsertedRecord(RecordMapper recordMapper) {
        Mockito.doAnswer(invocation -> {
            EncryptedRecordEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            when(recordMapper.selectRecord(entity.getTableName(), entity.getRecordId())).thenReturn(entity);
            return null;
        }).when(recordMapper).insertRecord(Mockito.any(EncryptedRecordEntity.class));
    }
}
