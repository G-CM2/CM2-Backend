package com.cm2.entity.dto.summaryapi;

import lombok.Builder;

@Builder
public record EnvironmentVar(String key, String value) {
}
