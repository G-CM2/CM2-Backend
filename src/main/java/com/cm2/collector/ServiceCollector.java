package com.cm2.collector;

import com.cm2.entity.dto.serviceapi.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceCollector {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 1) 서비스 목록 조회
    public ServiceListResponse listServices() {
        List<ServiceOverview> list = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "service", "ls", "--format",
                    "{{.ID}}\t{{.Name}}\t{{.Mode}}\t{{.Replicas}}\t{{.Image}}"
            );
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split("\t", 5);
                    if (tokens.length < 5) continue;
                    String[] reps = tokens[3].split("/", 2);
                    int running = Integer.parseInt(reps[0]);
                    int desired = Integer.parseInt(reps[1]);
                    list.add(ServiceOverview.builder()
                            .id(tokens[0])
                            .name(tokens[1])
                            .mode(tokens[2])
                            .replicasDesired(desired)
                            .replicasRunning(running)
                            .image(tokens[4])
                            .build());
                }
            }

            int exit = process.waitFor();
            if (exit != 0) {
                log.warn("docker service ls 명령 비정상 종료: exit code={}", exit);
            }
        } catch (Exception e) {
            log.error("Service list 조회 실패", e);
            throw new RuntimeException("Service list 조회 실패", e);
        }

        return ServiceListResponse.builder()
                .total(list.size())
                .services(list)
                .build();
    }

    // Swarm 모드 활성화 체크 (비활성화 상태면 자동으로 초기화)
    private void ensureSwarmInitialized() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "docker", "info", "--format", "{{.Swarm.LocalNodeState}}"
        );
        Process process = builder.start();
        process.waitFor();
        String state = new BufferedReader(new InputStreamReader(process.getInputStream()))
                .readLine();
        if (!"active".equalsIgnoreCase(state)) {
            new ProcessBuilder("docker", "swarm", "init").start().waitFor();
            log.info("Swarm mode automatically initialized");
        }
    }

    // 2) 서비스 생성
    public String createService(ServiceRequest req) {
        try {
            // Swarm 초기화 체크
            ensureSwarmInitialized();

            // 실제 서비스 생성
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "service", "create",
                    "--name", req.name(),
                    "--replicas", String.valueOf(req.replicas()),
                    req.image()
            );
            Process process = builder.start();

            // stderr 먼저 확인
            try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errLine = err.readLine();
                if (errLine != null && !errLine.isBlank()) {
                    throw new RuntimeException("Service 생성 실패: " + errLine);
                }
            }

            // stdout에서 ID 읽기
            try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String id = out.readLine();
                if (id == null || id.isBlank()) {
                    throw new RuntimeException("Service 생성 실패: ID를 찾을 수 없습니다.");
                }
                return id;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Service 생성 중 오류", e);
            throw new RuntimeException("Service 생성 실패: " + e.getMessage(), e);
        }
    }

    // 3) 서비스 상세 정보 및 태스크 조회
    public ServiceDetail getServiceDetail(String serviceId) {
        try {
            // ServiceInspect
            ProcessBuilder inspectBuilder = new ProcessBuilder(
                    "docker", "service", "inspect", serviceId, "--format", "{{json .Spec}}"
            );
            Process inspectProcess = inspectBuilder.start();
            StringBuilder specJson = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(inspectProcess.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) specJson.append(line);
            }
            inspectProcess.waitFor();
            JsonNode specNode = MAPPER.readTree(specJson.toString());
            var overview = listServices().services().stream()
                    .filter(s -> s.id().equals(serviceId))
                    .findFirst()
                    .orElseThrow();
            // TaskStatus 조회
            List<TaskStatus> tasks = new ArrayList<>();
            ProcessBuilder psBuilder = new ProcessBuilder(
                    "docker", "service", "ps", serviceId, "--format",
                    "{{.ID}}\t{{.Name}}\t{{.CurrentState}}\t{{.DesiredState}}"
            );
            Process psProcess = psBuilder.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String[] tok = line.split("\t", 4);
                    if (tok.length < 4) continue;
                    tasks.add(TaskStatus.builder()
                            .id(tok[0])
                            .name(tok[1])
                            .currentState(tok[2])
                            .desiredState(tok[3])
                            .build());
                }
            }
            psProcess.waitFor();
            return ServiceDetail.builder()
                    .id(overview.id())
                    .name(overview.name())
                    .mode(overview.mode())
                    .replicasDesired(overview.replicasDesired())
                    .replicasRunning(tasks.size())
                    .image(overview.image())
                    .constraints(List.of())
                    .resources(Resources.builder()
                            .limits(Limits.builder()
                                    .memory("512M")
                                    .cpu("0.5")
                                    .build())
                            .build())
                    .tasks(tasks)
                    .build();
        } catch (Exception e) {
            log.error("Service 상세 조회 실패", e);
            throw new RuntimeException("Service 상세 조회 실패", e);
        }
    }

    // 4) 서비스 삭제
    public void removeService(String serviceId) {
        try {
            new ProcessBuilder("docker", "service", "rm", serviceId).start().waitFor();
        } catch (Exception e) {
            log.error("Service 삭제 실패", e);
            throw new RuntimeException("Service 삭제 실패: " + e.getMessage(), e);
        }
    }

    // 5) 서비스 스케일링
    public void scaleService(String serviceId, ScaleRequest req) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "service", "scale",
                    serviceId + "=" + req.replicas()
            );
            Process process = builder.start();

            // 프로세스 종료 대기 및 exit code 확인
            int exit = process.waitFor();
            if (exit != 0) {
                log.error("Service 스케일링 실패: exit code={}", exit);
                throw new RuntimeException("Service 스케일링 실패: exit code=" + exit);
            }
        } catch (Exception e) {
            log.error("Service 스케일링 실패", e);
            throw new RuntimeException("Service 스케일링 실패", e);
        }
    }

    // 6) 롤링 업데이트 실행
    public String rollingUpdateService(String serviceId) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "docker", "service", "update",
                    "--force",
                    serviceId
            );
            Process process = builder.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = out.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("Rolling update 실패: exit code=" + exit);
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Rolling update 실패", e);
            throw new RuntimeException("Rolling update 실패", e);
        }
    }
}
