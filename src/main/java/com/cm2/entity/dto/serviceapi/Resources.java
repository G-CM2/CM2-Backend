package com.cm2.entity.dto.serviceapi;

import lombok.Builder;

@Builder
public record Resources(
        Limits limits
) {}
