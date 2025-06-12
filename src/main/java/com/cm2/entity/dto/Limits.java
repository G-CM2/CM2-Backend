package com.cm2.entity.dto;

import lombok.Builder;

@Builder
public record Limits(
        String memory,
        String cpu
) {}
