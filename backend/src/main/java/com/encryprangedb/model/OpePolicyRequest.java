package com.encryprangedb.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OpePolicyRequest(
        @NotBlank String policyName,
        @Min(0) int sensitivity,
        @Valid @NotEmpty List<Segment> segments
) {
    public record Segment(@Min(Long.MIN_VALUE) long minValue,
                          @Min(1) long a,
                          long b,
                          String label) {
    }
}
