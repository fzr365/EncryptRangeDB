package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.mapper.EafsAnchorMapper;
import com.encryprangedb.mapper.EafsOrderedNodeMapper;
import com.encryprangedb.mapper.RecordMapper;
import com.encryprangedb.model.entity.EafsOrderedNodeEntity;
import com.encryprangedb.model.entity.EncryptedIndexEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EafsOrderedIndexServiceTest {

    @Test
    void insertOrderedShouldRefreshAnchorsFromAffectedBlock() {
        EafsOrderedNodeMapper nodeMapper = Mockito.mock(EafsOrderedNodeMapper.class);
        EafsAnchorMapper anchorMapper = Mockito.mock(EafsAnchorMapper.class);
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        EafsOrderedIndexService service = new EafsOrderedIndexService(nodeMapper, anchorMapper, recordMapper, buildProps(4), integrityService());

        EafsOrderedNodeEntity predecessor = new EafsOrderedNodeEntity();
        predecessor.setId(10L);
        predecessor.setChainOrder(6L);
        predecessor.setNextNodeId(11L);
        when(nodeMapper.findByBucketAndRecordId("employees:salary", "emp-3")).thenReturn(null);
        when(nodeMapper.findPredecessor("employees:salary", 250L)).thenReturn(predecessor);

        EafsOrderedNodeEntity successor = new EafsOrderedNodeEntity();
        successor.setId(11L);
        successor.setChainOrder(7L);
        successor.setPrevNodeId(10L);
        when(nodeMapper.findById(11L)).thenReturn(successor);

        EafsOrderedNodeEntity refreshed7 = new EafsOrderedNodeEntity();
        refreshed7.setId(100L);
        refreshed7.setChainOrder(7L);
        refreshed7.setRindex(250L);
        EafsOrderedNodeEntity refreshed9 = new EafsOrderedNodeEntity();
        refreshed9.setId(11L);
        refreshed9.setChainOrder(9L);
        refreshed9.setRindex(300L);
        when(nodeMapper.listByBucketFromOrder("employees:salary", 5L)).thenReturn(List.of(refreshed7, refreshed9));

        service.insertOrdered("employees", "salary", 250L, "emp-3");

        verify(nodeMapper).shiftChainOrdersToNegative("employees:salary", 7L);
        verify(nodeMapper).restoreShiftedChainOrders("employees:salary", 7L);
        verify(anchorMapper).deleteFromOrder("employees:salary", 5L);
        verify(anchorMapper, never()).deleteByBucket("employees:salary");
    }

    @Test
    void rebuildAllBucketsShouldDeleteAndMaterializeEachBucket() {
        EafsOrderedNodeMapper nodeMapper = Mockito.mock(EafsOrderedNodeMapper.class);
        EafsAnchorMapper anchorMapper = Mockito.mock(EafsAnchorMapper.class);
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        EafsOrderedIndexService service = new EafsOrderedIndexService(nodeMapper, anchorMapper, recordMapper, buildProps(3), integrityService());

        Map<String, Object> bucket = new HashMap<>();
        bucket.put("tableName", "employees");
        bucket.put("columnName", "salary");
        when(recordMapper.listIndexedBuckets()).thenReturn(List.of(bucket));

        EncryptedIndexEntity idx1 = new EncryptedIndexEntity();
        idx1.setRecordId("emp-1");
        idx1.setRindex(100L);
        EncryptedIndexEntity idx2 = new EncryptedIndexEntity();
        idx2.setRecordId("emp-2");
        idx2.setRindex(200L);
        when(recordMapper.selectIndexRows("employees", "salary")).thenReturn(List.of(idx1, idx2));

        ArgumentCaptor<com.encryprangedb.model.entity.EafsOrderedNodeEntity> nodeCaptor =
                ArgumentCaptor.forClass(com.encryprangedb.model.entity.EafsOrderedNodeEntity.class);

        List<String> rebuilt = service.rebuildAllBuckets();

        assertEquals(List.of("employees:salary"), rebuilt);
        verify(anchorMapper).deleteByBucket("employees:salary");
        verify(nodeMapper).deleteByBucket("employees:salary");
        verify(nodeMapper, times(2)).insertNode(nodeCaptor.capture());
        assertEquals(List.of("emp-1", "emp-2"),
                nodeCaptor.getAllValues().stream().map(EafsOrderedNodeEntity::getRecordId).toList());
    }

    @Test
    void rebuildAllBucketsShouldRejectTamperedIndexRows() {
        EafsOrderedNodeMapper nodeMapper = Mockito.mock(EafsOrderedNodeMapper.class);
        EafsAnchorMapper anchorMapper = Mockito.mock(EafsAnchorMapper.class);
        RecordMapper recordMapper = Mockito.mock(RecordMapper.class);
        IntegrityService integrityService = integrityService();
        EafsOrderedIndexService service = new EafsOrderedIndexService(nodeMapper, anchorMapper, recordMapper, buildProps(3), integrityService);

        Map<String, Object> bucket = new HashMap<>();
        bucket.put("tableName", "employees");
        bucket.put("columnName", "salary");
        when(recordMapper.listIndexedBuckets()).thenReturn(List.of(bucket));

        EncryptedIndexEntity idx = new EncryptedIndexEntity();
        idx.setTableName("employees");
        idx.setColumnName("salary");
        idx.setRecordId("emp-1");
        idx.setRindex(100L);
        idx.setKeyVersion("v1");
        idx.setIndexTag("bad-tag");
        when(recordMapper.selectIndexRows("employees", "salary")).thenReturn(List.of(idx));
        when(integrityService.verifyIndex("employees", "salary", "emp-1", 100L, "v1", "bad-tag")).thenReturn(false);

        assertThrows(IllegalStateException.class, service::rebuildAllBuckets);
    }

    private CryptoProperties buildProps(int bucketSize) {
        CryptoProperties props = new CryptoProperties();
        CryptoProperties.Crypto crypto = new CryptoProperties.Crypto();
        crypto.setAesMasterKey("0123456789abcdef0123456789abcdef");
        crypto.setOpeMasterKey("0123456789abcdef0123456789abcdef");
        crypto.setSensitivity(1);
        props.setCrypto(crypto);

        CryptoProperties.Eafs eafs = new CryptoProperties.Eafs();
        eafs.setBucketSize(bucketSize);
        props.setEafs(eafs);
        return props;
    }

    private IntegrityService integrityService() {
        IntegrityService integrityService = Mockito.mock(IntegrityService.class);
        when(integrityService.verifyIndex(any(), any(), any(), any(Long.class), any(), any())).thenReturn(true);
        return integrityService;
    }
}
