package com.encryprangedb.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminSchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(AdminSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public AdminSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS app_user (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(64) NOT NULL,
                        password_hash VARCHAR(128) NOT NULL,
                        salt VARCHAR(64) NOT NULL,
                        role VARCHAR(32) NOT NULL,
                        enabled TINYINT(1) NOT NULL DEFAULT 1,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_app_user_username (username)
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS ope_policy_config (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        policy_name VARCHAR(128) NOT NULL,
                        sensitivity INT NOT NULL DEFAULT 1,
                        segment_json JSON NOT NULL,
                        active_flag TINYINT(1) NOT NULL DEFAULT 1,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS query_audit_log (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        action_type VARCHAR(64) NOT NULL,
                        sql_text TEXT NULL,
                        table_name VARCHAR(128) NULL,
                        column_name VARCHAR(128) NULL,
                        lower_index BIGINT NULL,
                        upper_index BIGINT NULL,
                        hit_count INT NULL,
                        elapsed_ms BIGINT NULL,
                        status VARCHAR(32) NOT NULL,
                        detail_text TEXT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS eafs_ordered_node (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        bucket VARCHAR(128) NOT NULL,
                        chain_order BIGINT NOT NULL,
                        record_id VARCHAR(128) NOT NULL,
                        rindex BIGINT NOT NULL,
                        prev_node_id BIGINT NULL,
                        next_node_id BIGINT NULL,
                        chain_key_hex VARCHAR(128) NOT NULL,
                        payload_base64 TEXT NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_ordered_bucket_record (bucket, record_id),
                        UNIQUE KEY uk_ordered_bucket_order (bucket, chain_order),
                        KEY idx_ordered_bucket_rindex (bucket, rindex, id),
                        KEY idx_ordered_bucket_prev (bucket, prev_node_id),
                        KEY idx_ordered_bucket_next (bucket, next_node_id)
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS eafs_anchor (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        bucket VARCHAR(128) NOT NULL,
                        anchor_order BIGINT NOT NULL,
                        anchor_rindex BIGINT NOT NULL,
                        node_id BIGINT NOT NULL,
                        UNIQUE KEY uk_anchor_bucket_node (bucket, node_id),
                        KEY idx_anchor_bucket_rindex (bucket, anchor_rindex, anchor_order)
                    )
                    """);
            addColumnIfMissing("encrypted_record", "integrity_tag", "VARCHAR(128) NULL");
            addColumnIfMissing("encrypted_record", "key_version", "VARCHAR(32) NOT NULL DEFAULT 'v1'");
            addColumnIfMissing("encrypted_index", "index_tag", "VARCHAR(128) NULL");
            addColumnIfMissing("encrypted_index", "key_version", "VARCHAR(32) NOT NULL DEFAULT 'v1'");
        } catch (Exception ex) {
            log.warn("Admin schema initializer skipped: {}", ex.getMessage());
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer tableCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                        """,
                Integer.class, tableName);
        if (tableCount == null || tableCount == 0) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
