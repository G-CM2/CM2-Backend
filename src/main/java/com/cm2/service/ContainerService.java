package com.cm2.service;

import com.cm2.entity.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ContainerService {

    // 모든 컨테이너 목록 조회 API용
    public ContainerInfoResponse getContainerInfo(String namespace, String status, int limit, int page) {
        List<ContainerDetail> allContainers = new ArrayList<>();
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
                // 목록 조회에서는 생성일, health, cpu_usage, memory_usage, restart_count 등에 대해
                // 추후 docker inspect 를 통해 보완 가능하나, 일단은 예시값 혹은 기본값을 사용합니다.
                String createdAt = "2025-03-28T10:15:30Z";
                String health = "healthy";
                double cpuUsage = 12.5;
                int memoryUsage = 256;
                int restartCount = 0;

                // 필터링: 네임스페이스 필터 (컨테이너 이름에 namespace 문자열 포함 여부)
                if (namespace != null && !namespace.isEmpty() && !name.contains(namespace)) continue;
                // 필터링: status 필터 (대소문자 구분 없이)
                if (status != null && !status.isEmpty() && !state.equalsIgnoreCase(status)) continue;

                ContainerDetail detail = ContainerDetail.builder()
                        .id(containerId)
                        .name(name)
                        .image(image)
                        .status(state)
                        .createdAt(createdAt)
                        .health(health)
                        .cpuUsage(cpuUsage)
                        .memoryUsage(memoryUsage)
                        .restartCount(restartCount)
                        .build();

                allContainers.add(detail);
            }
        } catch (IOException e) {
            log.error("컨테이너 정보 수집 중 오류 발생", e);
            throw new RuntimeException("컨테이너 정보 획득 실패", e);
        }

        int total = allContainers.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(page * limit, total);
        List<ContainerDetail> pagedList = allContainers.subList(fromIndex, toIndex);

        return ContainerInfoResponse.builder()
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


                String logs = "컨테이너 로그 샘플 데이터...";

                return ContainerDetail.builder()
                        .id(id)
                        .name(name)
                        .image(image)
                        .status(state)
                        .createdAt(createdAt)
                        .build();
            }
            return null;
        } catch (IOException e) {
            log.error("컨테이너 상세 정보 조회 중 오류 발생", e);
            throw new RuntimeException("컨테이너 상세 정보 획득 실패", e);
        }
    }

}
