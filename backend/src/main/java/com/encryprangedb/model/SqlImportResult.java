package com.encryprangedb.model;

import java.util.List;

public record SqlImportResult(
        int totalStatements,
        int handledInsertStatements,
        int insertedRows,
        List<String> errors
) {
}

