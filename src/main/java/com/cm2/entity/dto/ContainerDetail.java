package com.cm2.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
public class ContainerDetail {
    private String id;
    private String name;
    private String image;
    private String status;
    private String createdAt;
    private String health;
    private double cpuUsage;
    private int memoryUsage;
    private int restartCount;

    // 추가 정보 : 포트, 볼륨, 환경 변수, 로그
    @Builder.Default
    private List<PortMapping> ports = Collections.emptyList();
    @Builder.Default
    private List<VolumeMapping> volumes = Collections.emptyList();
    @Builder.Default
    private List<EnvironmentVar> environment = Collections.emptyList();
    @Builder.Default
    private String logs = "";

    @Builder
    public ContainerDetail(String id, String name, String image, String status, String createdAt,
                           String health, double cpuUsage, int memoryUsage, int restartCount,
                           List<PortMapping> ports, List<VolumeMapping> volumes,
                           List<EnvironmentVar> environment, String logs) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.status = status;
        this.createdAt = createdAt;
        this.health = health;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.restartCount = restartCount;
        this.ports = ports == null ? Collections.emptyList() : ports;
        this.volumes = volumes == null ? Collections.emptyList() : volumes;
        this.environment = environment == null ? Collections.emptyList() : environment;
        this.logs = logs == null ? "" : logs;
    }
}
