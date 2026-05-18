package com.encryprangedb.model;

import java.time.OffsetDateTime;
import java.util.List;

public record OpePolicyResponse(
        Long id,
        String policyName,
        int sensitivity,
        List<OpePolicyRequest.Segment> segments,
        boolean active,
        OffsetDateTime updatedAt
) {
}
