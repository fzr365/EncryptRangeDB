package com.encryprangedb.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlainInsertRequest(
        @NotBlank String table,
        @NotBlank String recordId,
        @NotEmpty List<Field> fields
) {
    public record Field(
            @NotBlank String column,
            @NotNull Object value,
            boolean indexed
    ) {
    }
}

