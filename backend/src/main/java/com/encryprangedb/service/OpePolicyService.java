package com.encryprangedb.service;

import com.encryprangedb.config.CryptoProperties;
import com.encryprangedb.crypto.HmacUtil;
import com.encryprangedb.mapper.AnalyticsMapper;
import com.encryprangedb.model.OpePolicyRequest;
import com.encryprangedb.model.OpePolicyResponse;
import com.encryprangedb.model.entity.OpePolicyEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class OpePolicyService {
    private static final Logger log = LoggerFactory.getLogger(OpePolicyService.class);

    private final AnalyticsMapper analyticsMapper;
    private final CryptoProperties cryptoProperties;
    private final ObjectMapper objectMapper;
    private final byte[] opeKey;

    public OpePolicyService(AnalyticsMapper analyticsMapper, CryptoProperties cryptoProperties, ObjectMapper objectMapper) {
        this.analyticsMapper = analyticsMapper;
        this.cryptoProperties = cryptoProperties;
        this.objectMapper = objectMapper;
        this.opeKey = cryptoProperties.getCrypto().getOpeMasterKey().getBytes(StandardCharsets.UTF_8);
    }

    public long encrypt(long value) {
        // 先按数值范围选择策略段，再计算保序索引。
        PolicyRuntime runtime = resolveRuntime();
        OpePolicyRequest.Segment segment = runtime.segments().stream()
                .filter(seg -> value >= seg.minValue())
                .max(Comparator.comparingLong(OpePolicyRequest.Segment::minValue))
                .orElse(runtime.segments().get(0));
        long skindex = Math.addExact(Math.multiplyExact(segment.a(), value), segment.b());
        long bound = Math.max(0L, Math.multiplyExact(segment.a(), runtime.sensitivity()));
        // 加一点确定性噪声，结果仍然可重复，便于索引查询。
        return Math.addExact(skindex, boundedNoise("default", value, bound));
    }

    public OpePolicyResponse getActivePolicy() {
        PolicyRuntime runtime = resolveRuntime();
        return new OpePolicyResponse(runtime.id(), runtime.policyName(), runtime.sensitivity(), runtime.segments(), true, runtime.updatedAt());
    }

    @Transactional
    public OpePolicyResponse saveActivePolicy(OpePolicyRequest request) {
        try {
            analyticsMapper.deactivatePolicies();
            OpePolicyEntity entity = new OpePolicyEntity();
            entity.setPolicyName(request.policyName());
            entity.setSensitivity(request.sensitivity());
            entity.setSegmentJson(toJson(request.segments()));
            entity.setActiveFlag(true);
            analyticsMapper.insertPolicy(entity);
            OpePolicyEntity saved = analyticsMapper.activePolicy();
            if (saved != null) {
                return toResponse(saved);
            }
        } catch (Exception ex) {
            log.warn("Save OPE policy failed, fallback to request runtime: {}", ex.getMessage());
        }
        return new OpePolicyResponse(null, request.policyName(), request.sensitivity(), request.segments(), true, null);
    }

    private long boundedNoise(String column, long value, long boundInclusive) {
        if (boundInclusive <= 0) {
            return 0L;
        }
        byte[] msg = (column + ":" + value).getBytes(StandardCharsets.UTF_8);
        byte[] mac = HmacUtil.hmacSha256(opeKey, msg);
        long seed = ByteBuffer.wrap(mac, 0, 8).getLong() & Long.MAX_VALUE;
        return seed % (boundInclusive + 1);
    }

    private PolicyRuntime resolveRuntime() {
        // 优先使用数据库里的当前策略，失败时回退到默认策略。
        try {
            OpePolicyEntity entity = analyticsMapper.activePolicy();
            if (entity == null) {
                return defaultRuntime();
            }
            return new PolicyRuntime(
                    entity.getId(),
                    entity.getPolicyName(),
                    entity.getSensitivity(),
                    parseSegments(entity.getSegmentJson()),
                    entity.getUpdatedAt()
            );
        } catch (Exception ex) {
            log.warn("Load OPE policy failed, fallback to default runtime: {}", ex.getMessage());
            return defaultRuntime();
        }
    }

    private PolicyRuntime defaultRuntime() {
        return new PolicyRuntime(
                null,
                "default-policy",
                cryptoProperties.getCrypto().getSensitivity(),
                List.of(
                        new OpePolicyRequest.Segment(0, 3, 10, "low-range"),
                        new OpePolicyRequest.Segment(100, 5, 20, "mid-range"),
                        new OpePolicyRequest.Segment(500, 10, 30, "high-range")
                ),
                null
        );
    }

    private OpePolicyResponse toResponse(OpePolicyEntity entity) {
        return new OpePolicyResponse(
                entity.getId(),
                entity.getPolicyName(),
                entity.getSensitivity(),
                parseSegments(entity.getSegmentJson()),
                Boolean.TRUE.equals(entity.getActiveFlag()),
                entity.getUpdatedAt()
        );
    }

    private List<OpePolicyRequest.Segment> parseSegments(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Parse policy segment json failed", e);
        }
    }

    private String toJson(List<OpePolicyRequest.Segment> segments) {
        try {
            return objectMapper.writeValueAsString(segments);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serialize policy failed", e);
        }
    }

    private record PolicyRuntime(
            Long id,
            String policyName,
            int sensitivity,
            List<OpePolicyRequest.Segment> segments,
            OffsetDateTime updatedAt
    ) {
    }
}
