package com.cm2.collector;

import com.cm2.entity.dto.clusterapi.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterCollector {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 클러스터 상태 및 토폴로지 정보 조회
    public ClusterStatusResponse getClusterStatus() {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "info", "--format", "{{json .Swarm}}"
            );
            Process process = builder.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            int exit = process.waitFor();
            if (exit != 0) {
                log.warn("docker info --format '{{json .Swarm}}' 비정상 종료: exit code={}", exit);
            }

            JsonNode root = MAPPER.readTree(sb.toString());
            JsonNode clusterNode = root.path("Cluster");

            String clusterID = clusterNode.path("ID").asText();
            String name = clusterNode.path("Spec").path("Name").asText();
            String orchestration = clusterNode
                    .path("Spec").path("Orchestration").path("TaskHistoryRetentionLimit").asText();
            String raftStatus = clusterNode.path("Raft").path("SnapshotIntervalIndex").asText();

            List<NodeInfo> nodes = listNodes().nodes();

            return ClusterStatusResponse.builder()
                    .clusterID(clusterID)
                    .name(name)
                    .orchestration(orchestration)
                    .raftStatus(raftStatus)
                    .nodes(nodes)
                    .build();
        } catch (Exception e) {
            log.error("Cluster status 조회 실패", e);
            throw new RuntimeException("Cluster status 조회 실패", e);
        }
    }

    // 노드 목록 및 상세 상태 조회
    public NodeListResponse listNodes() {
        List<NodeInfo> nodes = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "node", "ls", "--format",
                    "{{.ID}}	{{.Hostname}}	{{.Status}}	{{.Availability}}	{{.ManagerStatus}}"
            );
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tok = line.split("	", 5);
                    if (tok.length < 5) continue;
                    nodes.add(NodeInfo.builder()
                            .id(tok[0])
                            .hostname(tok[1])
                            .status(tok[2])
                            .availability(tok[3])
                            .managerStatus(tok[4])
                            .build());
                }
            }
            int exit = process.waitFor();
            if (exit != 0) log.warn("docker node ls 비정상 종료: exit code={}", exit);
        } catch (Exception e) {
            log.error("Node list 조회 실패", e);
            throw new RuntimeException("Node list 조회 실패", e);
        }
        return NodeListResponse.builder().total(nodes.size()).nodes(nodes).build();
    }


    // 특정 노드 드레인 실행
    public void drainNode(String nodeId) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "node", "update", "--availability", "drain", nodeId
            );
            Process process = builder.start();
            int exit = process.waitFor();
            if (exit !=0) throw new RuntimeException("Drain node 실패: exit code="+exit);
        } catch (Exception e) {
            log.error("Drain node 실패", e);
            throw new RuntimeException("Drain node 실패", e);
        }
    }

    // 클러스터 헬스체크 정보 조회
    public HealthStatusResponse getClusterHealth() {
        NodeListResponse nl = listNodes();
        int total = nl.total();
        int managers = (int) nl.nodes().stream()
                .filter(n-> n.managerStatus()!=null && !n.managerStatus().isBlank())
                .count();
        int unreachable = (int) nl.nodes().stream()
                .filter(n -> !"Ready".equalsIgnoreCase(n.status()))
                .count();

        return HealthStatusResponse.builder()
                .totalNodes(total)
                .activeManagers(managers)
                .unreachableNodes(unreachable)
                .build();
    }

    // 장애 시뮬레이션 실행
    public void simulateFailure(SimulationRequest req) {
        String nodeId = req.nodeId();
        String type = Optional.ofNullable(req.failureType())
                .map(String::toLowerCase)
                .orElse("drain");
        try {
            ProcessBuilder builder;
            switch (type) {
                case "drain":
                    builder = new ProcessBuilder(
                            "docker", "node", "update", "--availability", "drain", nodeId
                    );
                    break;
                case "activate":
                    builder = new ProcessBuilder(
                            "docker", "node", "update", "--availability", "active", nodeId
                    );
                    break;
                case "pause":
                    builder = new ProcessBuilder(
                            "docker", "node", "update", "--availability", "pause", nodeId
                    );
                    break;
                case "network-isolate":
                    builder = new ProcessBuilder(
                            "docker", "network", "disconnect", "overlay", nodeId, "<container-id>"
                    );
                    break;
                case "throttle-cpu":
                    builder = new ProcessBuilder(
                            "docker", "update", "--cpu-quota", "50000", nodeId
                    );
                    break;
                default:
                    throw new IllegalArgumentException("지원되지 않는 failureType: " + type);
            }
            Process process = builder.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("simulateFailure 실패 [" + type + "]: exit code=" + exit);
            }
        } catch (Exception e) {
            log.error("simulateFailure 실패", e);
            throw new RuntimeException("simulateFailure 실패: " + e.getMessage(), e);
        }
    }

}
