package com.cm2.entity.dto.clusterapi;

import lombok.Builder;

@Builder
public record SimulationRequest(
        String nodeId,
        String failureType
) {}
