package com.encryprangedb.model;

import java.util.List;
import java.util.Map;

public record RangeQueryResponse(List<Map<String, Object>> rows) {
}

