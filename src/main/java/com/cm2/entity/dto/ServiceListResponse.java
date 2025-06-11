package com.cm2.entity.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record ServiceListResponse(
        int total,
        List<ServiceOverview> services
) {}
