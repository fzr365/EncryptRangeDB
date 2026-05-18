package com.encryprangedb.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RangeQueryRequest(
        @NotBlank String table,
        @NotBlank String column,
        @NotNull Long lowerIndex,
        @NotNull Long upperIndex
) {
}

