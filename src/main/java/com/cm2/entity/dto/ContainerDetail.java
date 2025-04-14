package com.cm2.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Builder
    public ContainerDetail(String id, String name, String image, String status, String createdAt,
                           String health, double cpuUsage, int memoryUsage, int restartCount) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.status = status;
        this.createdAt = createdAt;
        this.health = health;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.restartCount = restartCount;
    }
}
