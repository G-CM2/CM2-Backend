package com.cm2.entity.dto;

import com.cm2.collector.dto.ContainerSummary;
import com.cm2.collector.dto.ResourceSummary;
import com.cm2.collector.dto.StatusSummary;
import lombok.Builder;

@Builder
public record SummaryResponse(StatusSummary status, ContainerSummary containers, ResourceSummary resources, String updatedAt) {
}
