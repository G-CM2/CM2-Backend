package com.cm2.entity.dto.clusterapi;

import lombok.Builder;

import java.util.List;

@Builder
public record NodeListResponse(
        int total,
        List<NodeInfo> nodes
) {}
