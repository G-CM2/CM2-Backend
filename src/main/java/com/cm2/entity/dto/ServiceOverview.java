package com.cm2.entity.dto;

import lombok.Builder;

@Builder
public record ServiceOverview(
        String id,
        String name,
        String mode,
        int replicasDesired,
        int replicasRunning,
        String image
) {}
