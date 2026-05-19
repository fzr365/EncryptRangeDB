package com.encryprangedb.mapper;

import com.encryprangedb.model.entity.EncryptedIndexEntity;
import com.encryprangedb.model.entity.EncryptedRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecordMapper {

    void insertRecord(EncryptedRecordEntity entity);

    void updateIntegrityTag(@Param("id") Long id,
                            @Param("integrityTag") String integrityTag);

    void insertIndex(EncryptedIndexEntity entity);

    void updateIndexIntegrity(@Param("id") Long id,
                              @Param("indexTag") String indexTag,
                              @Param("keyVersion") String keyVersion);

    List<Map<String, Object>> listIndexedBuckets();

    EncryptedRecordEntity selectRecord(@Param("table") String table,
                                       @Param("recordId") String recordId);

    List<EncryptedRecordEntity> selectAllRecords();

    List<EncryptedIndexEntity> selectIndexRows(@Param("table") String table,
                                               @Param("column") String column);

    List<EncryptedIndexEntity> selectAllIndexes();

    List<Map<String, Object>> selectByRange(@Param("table") String table,
                                            @Param("column") String column,
                                            @Param("lower") long lower,
                                            @Param("upper") long upper);

    List<Map<String, Object>> selectByRecordIds(@Param("table") String table,
                                                @Param("column") String column,
                                                @Param("recordIds") List<String> recordIds);

    List<Map<String, Object>> selectLatest(@Param("table") String table,
                                           @Param("column") String column,
                                           @Param("limit") int limit);
}
