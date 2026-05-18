package com.encryprangedb.mapper;

import com.encryprangedb.model.entity.EafsOrderedNodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EafsOrderedNodeMapper {
    Long countByBucket(@Param("bucket") String bucket);

    EafsOrderedNodeEntity findHead(@Param("bucket") String bucket);

    EafsOrderedNodeEntity findById(@Param("id") Long id);

    EafsOrderedNodeEntity findByBucketAndRecordId(@Param("bucket") String bucket, @Param("recordId") String recordId);

    EafsOrderedNodeEntity findPredecessor(@Param("bucket") String bucket, @Param("rindex") long rindex);

    List<EafsOrderedNodeEntity> listByBucketFromOrder(@Param("bucket") String bucket, @Param("fromOrder") long fromOrder);

    List<EafsOrderedNodeEntity> listByBucket(@Param("bucket") String bucket);

    void shiftChainOrdersToNegative(@Param("bucket") String bucket, @Param("fromOrder") long fromOrder);

    void restoreShiftedChainOrders(@Param("bucket") String bucket, @Param("fromOrder") long fromOrder);

    void deleteByBucket(@Param("bucket") String bucket);

    void insertNode(EafsOrderedNodeEntity entity);

    void updateLinks(@Param("id") Long id,
                     @Param("prevNodeId") Long prevNodeId,
                     @Param("nextNodeId") Long nextNodeId);
}
