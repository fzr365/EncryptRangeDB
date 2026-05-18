package com.encryprangedb.model;

public record RebuildEafsRequest(
        String table,
        String column,
        boolean rebuildAll
) {
}
