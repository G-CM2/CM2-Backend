package com.cm2.collector;

import com.cm2.collector.dto.StatsResult;
import com.cm2.entity.Action;
import com.cm2.entity.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DockerContainerCollector {

    private double parseCpuUsage(String cpuStr) {
        if (cpuStr == null) return 0.0;
        cpuStr = cpuStr.replace("%", "").trim();
        try {
            return Double.parseDouble(cpuStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int parseMemoryUsage(String memStr) {
        if (memStr == null) return 0;
        memStr = memStr.toUpperCase().trim();

        double value;
        if (memStr.endsWith("GIB"))
            value = Double.parseDouble(memStr.replace("GIB", "").trim()) * 1024;
        else if (memStr.endsWith("MIB"))
            value = Double.parseDouble(memStr.replace("MIB", "").trim());
        else if (memStr.endsWith("KIB"))
            value = Double.parseDouble(memStr.replace("KIB", "").trim()) / 1024;
        else {
            try {
                value = Double.parseDouble(memStr);
            } catch (Exception e) {
                value = 0.0;
            }
        }
        return (int) Math.round(value);
    }

    private StatsResult getStatsForContainer(String containerId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "stats", "--no-stream", containerId);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            //헤더 삭제용
            reader.readLine();
            String dataLine = reader.readLine();
            process.waitFor();

            if (dataLine != null && !dataLine.isEmpty()) {
                String[] tokens = dataLine.split("\\s+");

                double cpuUsage = parseCpuUsage(tokens[2]);
                int memoryUsage = parseMemoryUsage(tokens[3]);
                return new StatsResult(cpuUsage, memoryUsage);
            }
        } catch (IOException | InterruptedException e) {
            log.error("docker stats 명령어 실행 중 오류", e);
        }
        return new StatsResult(0.0, 0);
    }

    // 모든 컨테이너 목록 조회 API용
    public ContainerListResponse getContainerInfo(String namespace, String status, int limit, int page) {
        List<ContainerOverview> allOverviews = new ArrayList<>();
        try {
            // docker ps -a 명령어 실행
            ProcessBuilder builder = new ProcessBuilder("docker", "ps", "-a", "--no-trunc");
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            //CONTAINER ID...STATUS, PORTS, NAMES 제거용
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                if (tokens.length < 5) continue;  // 파싱 에러 방지

                String containerId = tokens[0];
                String image = tokens[1];
                String state = tokens[2];
                String name = tokens[tokens.length - 1];

                // 필터링: 네임스페이스 필터 (컨테이너 이름에 namespace 문자열 포함 여부)
                if (namespace != null && !namespace.isEmpty() && !name.contains(namespace)) continue;
                // 필터링: status 필터 (대소문자 구분 없이)
                if (status != null && !status.isEmpty() && !state.equalsIgnoreCase(status)) continue;

                ContainerDetail detail = getContainerDetail(containerId);
                if (detail == null) continue;

                ContainerOverview overview = ContainerOverview.builder()
                        .id(detail.id())
                        .name(detail.name())
                        .image(detail.image())
                        .status(detail.status())
                        .createdAt(detail.createdAt())
                        .health(detail.health())
                        .cpuUsage(detail.cpuUsage())
                        .memoryUsage(detail.memoryUsage())
                        .restartCount(detail.restartCount())
                        .build();

                allOverviews.add(overview);
            }
        } catch (IOException e) {
            log.error("컨테이너 정보 수집 중 오류 발생", e);
            throw new RuntimeException("컨테이너 정보 획득 실패", e);
        }

        int total = allOverviews.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(page * limit, total);
        List<ContainerOverview> pagedList = allOverviews.subList(fromIndex, toIndex);

        return ContainerListResponse.builder()
                .total(total)
                .page(page)
                .limit(limit)
                .containers(pagedList)
                .build();
    }

    // 특정 컨테이너 상세 조회 API용
    public ContainerDetail getContainerDetail(String containerId) {
        try {
            // docker inspect {containerId} 명령어 실행
            ProcessBuilder builder = new ProcessBuilder("docker", "inspect", containerId);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) jsonOutput.append(line);


            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonOutput.toString());
            if (root.isArray() && !root.isEmpty()) {
                JsonNode containerNode = root.get(0);
                String id = containerNode.path("Id").asText();
                String name = containerNode.path("Name").asText().replace("/", "");
                String image = containerNode.path("Config").path("Image").asText();
                String state = containerNode.path("State").path("Status").asText();
                String createdAt = containerNode.path("Created").asText();
                int restartCount = containerNode.path("RestartCount").asInt(0);

                String health = "healthy";
                JsonNode healthNode = containerNode.path("State").path("Health");
                if (!healthNode.isMissingNode() && !healthNode.isNull()) {
                    health = healthNode.path("Status").asText("healthy");
                }

                StatsResult stats = getStatsForContainer(containerId);
                double cpuUsage = stats.cpuUsage;
                int memoryUsage = stats.memoryUsage;

                List<PortMapping> ports = new ArrayList<>();
                JsonNode portsNode = containerNode.path("NetworkSettings").path("Ports");
                if (portsNode.isObject()) {
                    portsNode.fieldNames().forEachRemaining(portKey -> {
                        JsonNode hostBindingArray = portsNode.get(portKey);
                        int internalPort = Integer.parseInt(portKey.split("/")[0]);
                        if (hostBindingArray.isArray() && !hostBindingArray.isEmpty()) {
                            String hostPort = hostBindingArray.get(0).path("HostPort").asText();
                            int externalPort = Integer.parseInt(hostPort);
                            ports.add(PortMapping.builder().internal(internalPort).external(externalPort).build());
                        }
                    });
                }

                List<VolumeMapping> volumes = new ArrayList<>();
                JsonNode mountsNode = containerNode.path("Mounts");
                if (mountsNode.isArray()) {
                    for (JsonNode mount : mountsNode) {
                        String source = mount.path("Source").asText();
                        String target = mount.path("Destination").asText();
                        volumes.add(VolumeMapping.builder().source(source).target(target).build());
                    }
                }

                List<EnvironmentVar> environment = new ArrayList<>();
                JsonNode envNode = containerNode.path("Config").path("Env");
                if (envNode.isArray()) {
                    for (JsonNode env : envNode) {
                        String envStr = env.asText();
                        String[] kv = envStr.split("=", 2);
                        if (kv.length == 2) {
                            environment.add(EnvironmentVar.builder().key(kv[0]).value(kv[1]).build());
                        }
                    }
                }

                String log = containerNode.path("LogPath").asText();

                return ContainerDetail.builder()
                        .id(id)
                        .name(name)
                        .image(image)
                        .status(state)
                        .createdAt(createdAt)
                        .health(health)
                        .cpuUsage(cpuUsage)
                        .memoryUsage(memoryUsage)
                        .restartCount(restartCount)
                        .ports(ports)
                        .volumes(volumes)
                        .environment(environment)
                        .log(log)
                        .build();
            }
            return null;
        } catch (IOException e) {
            log.error("컨테이너 상세 정보 조회 중 오류 발생", e);
            throw new RuntimeException("컨테이너 상세 정보 획득 실패", e);
        }
    }

    public ActionResponse controlContainer(String containerId, String actionStr) {
        // 지원하는 동작 목록 : START, RESTART, KILL, STOP, DIE, DESTROY, CREATE
        Action action;
        try {
            action = Action.valueOf(actionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원되지 않는 동작: " + actionStr);
        }

        String dockerCommand;
        String statusResult;
        ProcessBuilder builder = switch (action) {
            case START -> {
                dockerCommand = "start";
                statusResult = "starting";
                yield new ProcessBuilder("docker", dockerCommand, containerId);
            }
            case RESTART -> {
                dockerCommand = "restart";
                statusResult = "restarting";
                yield new ProcessBuilder("docker", dockerCommand, containerId);
            }
            case KILL -> {
                dockerCommand = "kill";
                statusResult = "killing";
                yield new ProcessBuilder("docker", dockerCommand, containerId);
            }
            case STOP -> {
                dockerCommand = "stop";
                statusResult = "stopping";
                yield new ProcessBuilder("docker", dockerCommand, containerId);
            }
            case DIE -> {
                statusResult = "died";
                yield new ProcessBuilder("docker", "kill", "--signal=SIGTERM", containerId);
            }
            case DESTROY -> {
                dockerCommand = "rm";
                statusResult = "destroying";
                yield new ProcessBuilder("docker", dockerCommand, containerId);
            }
            case CREATE -> throw new UnsupportedOperationException("CREATE 동작은 기존 컨테이너 제어에서는 지원되지 않습니다.");
            default -> throw new IllegalArgumentException("지원되지 않는 동작: " + actionStr);
        };

        try {
            Process process = builder.start();

            try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = stdOut.readLine()) != null) output.append(line);

                StringBuilder errOutput = new StringBuilder();
                while ((line = stdErr.readLine()) != null) errOutput.append(line);

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    String message = "Container " + action.name().toLowerCase() + "ed successfully";
                    return ActionResponse.builder()
                            .success(true)
                            .message(message)
                            .status(statusResult)
                            .build();
                } else {
                    String errorMsg = errOutput.toString().isEmpty() ? "식별되지 않는 에러" : errOutput.toString();
                    throw new RuntimeException("Docker 명령어 실행 실패 : " + errorMsg);
                }
            }
        } catch (IOException | InterruptedException ex) {
            log.error("컨테이너 제어 동작 실행 중 오류", ex);
            throw new RuntimeException("컨테이너 제어 동작 실행 실패", ex);
        }
    }

}
