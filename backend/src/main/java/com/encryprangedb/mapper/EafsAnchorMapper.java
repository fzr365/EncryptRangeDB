package com.encryprangedb.mapper;

import com.encryprangedb.model.entity.EafsAnchorEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EafsAnchorMapper {
    EafsAnchorEntity findFloorAnchor(@Param("bucket") String bucket, @Param("rindex") long rindex);

    void deleteByBucket(@Param("bucket") String bucket);

    void deleteFromOrder(@Param("bucket") String bucket, @Param("fromOrder") long fromOrder);

    void insertAnchor(EafsAnchorEntity entity);

    List<EafsAnchorEntity> listByBucket(@Param("bucket") String bucket);
}
