package com.cm2.entity.dto.clusterapi;

import lombok.Builder;

@Builder
public record HealthStatusResponse(
        int totalNodes,
        int activeManagers,
        int unreachableNodes
) {}
