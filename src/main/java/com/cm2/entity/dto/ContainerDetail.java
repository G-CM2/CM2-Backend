package com.cm2.entity.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ContainerDetail(String id, String name, String image, String status, String createdAt, String health,
                              double cpuUsage, int memoryUsage, int restartCount, List<PortMapping> ports,
                              List<VolumeMapping> volumes, List<EnvironmentVar> environment, String log) {
    // 추가 정보 : 포트, 볼륨, 환경 변수, 로그
}
