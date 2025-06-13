package com.cm2.entity.dto.serviceapi;

import lombok.Builder;

import java.util.List;

@Builder
public record ServiceRequest(
        String name,
        String image,
        int replicas,
        List<String> constraints,
        Resources resources
) {}
