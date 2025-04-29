package com.cm2.collector.dto;

import lombok.Builder;

@Builder
public record ResourceSummary(double cpuUsage, double memoryUsage, double diskUsage) {
}
