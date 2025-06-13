package com.cm2.controller;

import com.cm2.collector.ClusterCollector;
import com.cm2.entity.dto.clusterapi.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cluster")
@RequiredArgsConstructor
public class ClusterController {
    private final ClusterCollector collector;

    // 클러스터 전체 상태 및 토폴로지 정보 조회
    @GetMapping("/status")
    public ResponseEntity<ClusterStatusResponse> status() {
        return ResponseEntity.ok(collector.getClusterStatus());
    }

    // 노드 목록 및 상세 상태 조회
    @GetMapping("/nodes")
    public ResponseEntity<NodeListResponse> nodes() {
        return ResponseEntity.ok(collector.listNodes());
    }

    // 특정 노드 드레인 실행
    @PostMapping("/nodes/{nodeId}/drain")
    public ResponseEntity<Void> drain(@PathVariable String nodeId) {
        collector.drainNode(nodeId);
        return ResponseEntity.ok().build();
    }

    // 클러스터 헬스체크 정보 조회
    @GetMapping("/health")
    public ResponseEntity<HealthStatusResponse> health() {
        return ResponseEntity.ok(collector.getClusterHealth());
    }

    // 장애 시뮬레이션 실행
    @PostMapping("/simulate/failure")
    public ResponseEntity<Void> simulate(@RequestBody SimulationRequest req) {
        collector.simulateFailure(req);
        return ResponseEntity.ok().build();
    }
}

