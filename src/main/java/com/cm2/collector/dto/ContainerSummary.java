package com.cm2.collector.dto;

import lombok.Builder;

@Builder
public record ContainerSummary(int total, int running, int stopped, int error) {
}
