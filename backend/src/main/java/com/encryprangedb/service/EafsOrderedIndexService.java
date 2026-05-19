package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.crypto.HashUtil;
import com.encryprangedb.crypto.HmacUtil;
import com.encryprangedb.mapper.EafsAnchorMapper;
import com.encryprangedb.mapper.EafsOrderedNodeMapper;
import com.encryprangedb.mapper.RecordMapper;
import com.encryprangedb.model.entity.EafsAnchorEntity;
import com.encryprangedb.model.entity.EafsOrderedNodeEntity;
import com.encryprangedb.model.entity.EncryptedIndexEntity;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EafsOrderedIndexService {

    private final EafsOrderedNodeMapper orderedNodeMapper;
    private final EafsAnchorMapper anchorMapper;
    private final RecordMapper recordMapper;
    private final IntegrityService integrityService;
    private final byte[] opeKey;
    private final int anchorStep;

    public EafsOrderedIndexService(EafsOrderedNodeMapper orderedNodeMapper,
                                   EafsAnchorMapper anchorMapper,
                                   RecordMapper recordMapper,
                                   CryptoProperties cryptoProperties,
                                   IntegrityService integrityService) {
        this.orderedNodeMapper = orderedNodeMapper;
        this.anchorMapper = anchorMapper;
        this.recordMapper = recordMapper;
        this.integrityService = integrityService;
        this.opeKey = cryptoProperties.getCrypto().getOpeMasterKey().getBytes(StandardCharsets.UTF_8);
        this.anchorStep = Math.max(1, cryptoProperties.getEafs().getBucketSize());
    }

    @Transactional
    public void insertOrdered(String table, String column, long rindex, String recordId) {
        String bucket = bucketOf(table, column);
        if (orderedNodeMapper.findByBucketAndRecordId(bucket, recordId) != null) {
            return;
        }

        EafsOrderedNodeEntity predecessor = orderedNodeMapper.findPredecessor(bucket, rindex);
        EafsOrderedNodeEntity successor = predecessor != null
                ? nullableNode(predecessor.getNextNodeId())
                : orderedNodeMapper.findHead(bucket);
        long chainOrder = predecessor != null ? predecessor.getChainOrder() + 1L : 1L;

        orderedNodeMapper.shiftChainOrdersToNegative(bucket, chainOrder);
        orderedNodeMapper.restoreShiftedChainOrders(bucket, chainOrder);

        EafsOrderedNodeEntity node = new EafsOrderedNodeEntity();
        node.setBucket(bucket);
        node.setChainOrder(chainOrder);
        node.setRecordId(recordId);
        node.setRindex(rindex);
        node.setPrevNodeId(predecessor == null ? null : predecessor.getId());
        node.setNextNodeId(successor == null ? null : successor.getId());
        node.setChainKeyHex(buildChainKey(bucket, recordId, rindex));
        node.setPayloadBase64(buildPayload(bucket, recordId, rindex));
        node.setCreatedAt(OffsetDateTime.now());
        orderedNodeMapper.insertNode(node);

        if (predecessor != null) {
            orderedNodeMapper.updateLinks(predecessor.getId(), predecessor.getPrevNodeId(), node.getId());
        }
        if (successor != null) {
            orderedNodeMapper.updateLinks(successor.getId(), node.getId(), successor.getNextNodeId());
        }
        refreshAnchorsFrom(bucket, anchorRefreshStartOrder(chainOrder));
    }

    @Transactional
    public void ensureBucketMaterialized(String table, String column) {
        String bucket = bucketOf(table, column);
        Long existing = orderedNodeMapper.countByBucket(bucket);
        if (existing != null && existing > 0) {
            return;
        }

        materializeBucket(table, column);
    }

    public List<ChainHit> scanRange(String table, String column, long lowerIndex, long upperIndex) {
        ensureBucketMaterialized(table, column);
        String bucket = bucketOf(table, column);

        EafsAnchorEntity anchor = anchorMapper.findFloorAnchor(bucket, lowerIndex);
        EafsOrderedNodeEntity node = anchor != null
                ? orderedNodeMapper.findById(anchor.getNodeId())
                : orderedNodeMapper.findHead(bucket);

        while (node != null && node.getRindex() < lowerIndex) {
            node = nullableNode(node.getNextNodeId());
        }

        List<ChainHit> hits = new ArrayList<>();
        while (node != null && node.getRindex() <= upperIndex) {
            hits.add(new ChainHit(node.getRecordId(), node.getRindex(), node.getChainOrder(),
                    node.getChainKeyHex(), node.getPrevNodeId(), node.getNextNodeId()));
            node = nullableNode(node.getNextNodeId());
        }
        return hits;
    }

    public Map<String, ChainPreview> previewByRecordIds(String table, String column, List<String> recordIds) {
        ensureBucketMaterialized(table, column);
        String bucket = bucketOf(table, column);
        Map<String, ChainPreview> out = new HashMap<>();
        for (String recordId : recordIds) {
            EafsOrderedNodeEntity node = orderedNodeMapper.findByBucketAndRecordId(bucket, recordId);
            if (node == null) {
                continue;
            }
            EafsOrderedNodeEntity prev = nullableNode(node.getPrevNodeId());
            EafsOrderedNodeEntity next = nullableNode(node.getNextNodeId());
            out.put(recordId, new ChainPreview(
                    node.getChainOrder(),
                    node.getChainKeyHex(),
                    prev == null ? null : prev.getRecordId(),
                    next == null ? null : next.getRecordId(),
                    node.getRindex()
            ));
        }
        return out;
    }

    @Transactional
    public void rebuildBucket(String table, String column) {
        String bucket = bucketOf(table, column);
        anchorMapper.deleteByBucket(bucket);
        orderedNodeMapper.deleteByBucket(bucket);
        materializeBucket(table, column, false);
    }

    @Transactional
    public List<String> rebuildAllBuckets() {
        List<String> rebuilt = new ArrayList<>();
        for (Map<String, Object> bucketRow : recordMapper.listIndexedBuckets()) {
            String table = String.valueOf(bucketRow.get("tableName"));
            String column = String.valueOf(bucketRow.get("columnName"));
            if (isBlank(table) || isBlank(column)) {
                continue;
            }
            rebuildBucket(table, column);
            rebuilt.add(bucketOf(table, column));
        }
        return rebuilt;
    }

    private void rebuildAnchors(String bucket) {
        rebuildAnchors(bucket, true);
    }

    private void rebuildAnchors(String bucket, boolean clearExistingFirst) {
        if (clearExistingFirst) {
            anchorMapper.deleteByBucket(bucket);
        }
        insertAnchors(bucket, orderedNodeMapper.listByBucket(bucket));
    }

    private void refreshAnchorsFrom(String bucket, long fromOrder) {
        anchorMapper.deleteFromOrder(bucket, fromOrder);
        insertAnchors(bucket, orderedNodeMapper.listByBucketFromOrder(bucket, fromOrder));
    }

    private void insertAnchors(String bucket, List<EafsOrderedNodeEntity> nodes) {
        for (EafsOrderedNodeEntity node : nodes) {
            if (node.getChainOrder() == null) {
                continue;
            }
            long order = node.getChainOrder();
            if (order == 1L || (order - 1L) % anchorStep == 0L) {
                EafsAnchorEntity anchor = new EafsAnchorEntity();
                anchor.setBucket(bucket);
                anchor.setAnchorOrder(node.getChainOrder());
                anchor.setAnchorRindex(node.getRindex());
                anchor.setNodeId(node.getId());
                anchorMapper.insertAnchor(anchor);
            }
        }
    }

    private void materializeBucket(String table, String column) {
        materializeBucket(table, column, true);
    }

    private void materializeBucket(String table, String column, boolean clearAnchorsBeforeRebuild) {
        String bucket = bucketOf(table, column);
        List<EncryptedIndexEntity> indexRows = recordMapper.selectIndexRows(table, column);
        if (indexRows.isEmpty()) {
            return;
        }

        EafsOrderedNodeEntity prev = null;
        long order = 1L;
        for (EncryptedIndexEntity idx : indexRows) {
            verifyIndexRow(idx);
            EafsOrderedNodeEntity node = new EafsOrderedNodeEntity();
            node.setBucket(bucket);
            node.setChainOrder(order++);
            node.setRecordId(idx.getRecordId());
            node.setRindex(idx.getRindex());
            node.setPrevNodeId(prev == null ? null : prev.getId());
            node.setNextNodeId(null);
            node.setChainKeyHex(buildChainKey(bucket, idx.getRecordId(), idx.getRindex()));
            node.setPayloadBase64(buildPayload(bucket, idx.getRecordId(), idx.getRindex()));
            node.setCreatedAt(OffsetDateTime.now());
            orderedNodeMapper.insertNode(node);
            if (prev != null) {
                orderedNodeMapper.updateLinks(prev.getId(), prev.getPrevNodeId(), node.getId());
            }
            prev = node;
        }
        rebuildAnchors(bucket, clearAnchorsBeforeRebuild);
    }

    private void verifyIndexRow(EncryptedIndexEntity idx) {
        if (idx.getRindex() == null) {
            throw new IllegalStateException("Encrypted index is missing rindex, recordId=" + idx.getRecordId());
        }
        String keyVersion = idx.getKeyVersion() == null || idx.getKeyVersion().isBlank() ? "v1" : idx.getKeyVersion();
        boolean ok = integrityService.verifyIndex(
                idx.getTableName(),
                idx.getColumnName(),
                idx.getRecordId(),
                idx.getRindex(),
                keyVersion,
                idx.getIndexTag());
        if (!ok) {
            throw new IllegalStateException("Encrypted index integrity check failed, recordId=" + idx.getRecordId());
        }
    }

    private long anchorRefreshStartOrder(long changedOrder) {
        long block = (changedOrder - 1L) / anchorStep;
        return block * anchorStep + 1L;
    }

    private String bucketOf(String table, String column) {
        return table + ":" + column;
    }

    private EafsOrderedNodeEntity nullableNode(Long id) {
        return id == null ? null : orderedNodeMapper.findById(id);
    }

    private String buildChainKey(String bucket, String recordId, long rindex) {
        byte[] mac = HmacUtil.hmacSha256(opeKey, (bucket + "|" + recordId + "|" + rindex).getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(HashUtil.sha256(mac));
    }

    private String buildPayload(String bucket, String recordId, long rindex) {
        String raw = bucket + "|" + recordId + "|" + rindex;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank() || Objects.equals(text, "null");
    }

    public record ChainHit(String recordId,
                           Long rindex,
                           Long chainOrder,
                           String chainKeyHex,
                           Long prevNodeId,
                           Long nextNodeId) {
    }

    public record ChainPreview(Long chainOrder,
                               String chainKeyHex,
                               String prevRecordId,
                               String nextRecordId,
                               Long rindex) {
    }
}
