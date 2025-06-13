package com.cm2.entity.dto.serviceapi;

import lombok.Builder;

@Builder
public record TaskStatus(
        String id,
        String name,
        String currentState,
        String desiredState
) {}
