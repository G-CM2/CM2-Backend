package com.cm2.entity.dto.serviceapi;

import lombok.Builder;

import java.util.List;

@Builder
public record ServiceDetail(
        String id,
        String name,
        String mode,
        int replicasDesired,
        int replicasRunning,
        String image,
        List<String> constraints,
        Resources resources,
        List<TaskStatus> tasks
) {}
