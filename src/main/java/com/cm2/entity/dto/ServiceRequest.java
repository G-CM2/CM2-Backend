package com.cm2.entity.dto;

import lombok.Builder;

@Builder
public record ServiceRequest(
        String name,
        String image,
        int replicas
) {}
