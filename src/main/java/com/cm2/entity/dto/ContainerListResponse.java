package com.cm2.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ContainerListResponse {
    private int total;
    private int page;
    private int limit;
    private List<ContainerOverview> containers;

    @Builder
    public ContainerListResponse(int total, int page, int limit, List<ContainerOverview> containers) {
        this.total = total;
        this.page = page;
        this.limit = limit;
        this.containers = containers;
    }
}
