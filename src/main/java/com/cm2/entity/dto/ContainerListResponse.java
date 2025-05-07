package com.cm2.entity.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ContainerListResponse(int total, int page, int limit, List<ContainerOverview> containers) {
}
