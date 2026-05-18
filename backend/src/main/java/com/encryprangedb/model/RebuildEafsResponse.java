package com.encryprangedb.model;

import java.util.List;

public record RebuildEafsResponse(
        int rebuiltBuckets,
        List<String> buckets
) {
}
