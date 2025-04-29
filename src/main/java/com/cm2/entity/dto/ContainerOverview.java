package com.cm2.entity.dto;

import lombok.Builder;

@Builder
public record ContainerOverview(String id, String name, String image, String status, String createdAt, String health,
                                double cpuUsage, int memoryUsage, int restartCount) {
}
