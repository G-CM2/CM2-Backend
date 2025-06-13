package com.cm2.entity.dto.clusterapi;

import lombok.Builder;

@Builder
public record NodeInfo(
        String id,
        String hostname,
        String status,
        String availability,
        String managerStatus
) {}
