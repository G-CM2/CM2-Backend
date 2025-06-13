package com.cm2.entity.dto.clusterapi;

import lombok.Builder;

import java.util.List;

@Builder
public record ClusterStatusResponse(
        String clusterID,
        String name,
        String orchestration,
        String raftStatus,
        List<NodeInfo> nodes
) {}
