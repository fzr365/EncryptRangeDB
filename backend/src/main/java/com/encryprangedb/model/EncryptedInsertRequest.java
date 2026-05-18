package com.encryprangedb.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EncryptedInsertRequest(
        @NotBlank String table,
        @NotBlank String recordId,
        @NotEmpty List<EncryptedField> fields
) {
}

