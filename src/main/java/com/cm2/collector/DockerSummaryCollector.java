package com.cm2.collector;

import com.cm2.collector.dto.ContainerSummary;
import com.cm2.collector.dto.Indicator;
import com.cm2.collector.dto.StatsResult;
import com.cm2.collector.dto.StatusSummary;
import com.cm2.collector.dto.ResourceSummary;
import com.cm2.entity.dto.SummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerSummaryCollector {

    private final DockerContainerCollector containerCollector;

    // 전체 시스템 상태 요약 정보 조회용
    public SummaryResponse getSystemSummary() {
        int total = 0, running = 0, stopped = 0, error = 0;

        List<String> containerIds = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", "-a", "--no-trunc");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                //헤더 날리기
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length < 3) continue;
                    String containerId = tokens[0];
                    String state = tokens[2];

                    containerIds.add(containerId);
                    total++;
                    switch (state.toLowerCase()) {
                        case "running" -> running++;
                        case "exited" -> stopped++;
                        default -> error++;
                    }
                }
            }
        } catch (IOException e) {
            log.error("시스템 요약을 위한 컨테이너 목록 조회 중 오류", e);
            throw new RuntimeException("시스템 요약 정보 획득 실패", e);
        }

        double totalCpu = 0;
        double totalMemory = 0;
        for (String id : containerIds) {
            StatsResult stats = containerCollector.getStatsForContainer(id);
            totalCpu += stats.cpuUsage;
            totalMemory += stats.memoryUsage;
        }

        double diskUsage = 62.1;

        StatusSummary status = StatusSummary.builder()
                .description("정상 작동 중")
                .indicator(Indicator.NORMAL)
                .build();

        ContainerSummary containers = ContainerSummary.builder()
                .total(total)
                .running(running)
                .stopped(stopped)
                .error(error)
                .build();

        ResourceSummary resources = ResourceSummary.builder()
                .cpuUsage(totalCpu)
                .memoryUsage(totalMemory)
                .diskUsage(diskUsage)
                .build();

        String updatedAt = ZonedDateTime.now().toString();

        return SummaryResponse.builder()
                .status(status)
                .containers(containers)
                .resources(resources)
                .updatedAt(updatedAt)
                .build();
    }


}
