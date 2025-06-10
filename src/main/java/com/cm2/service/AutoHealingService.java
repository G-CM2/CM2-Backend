package com.cm2.service;

import com.cm2.collector.DockerEventCollector;
import com.cm2.entity.Action;
import com.cm2.entity.ContainerEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoHealingService {

    private final int maxRetryCount = 3;         // 최대 재시도 횟수
    private final long retryDelaySeconds = 60;   // 재시작 지연 시간(초)


    private final Map<String, Long> manualStopTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Integer> restartCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        scheduleRestartForExistingExited();
    }

    @EventListener
    public void onContainerKilledByUser(ContainerEvent event) {
        // 컨테이너 이벤트 수신 시 처리
        if (event.action() == Action.KILL || event.action() == Action.STOP)
            manualStopTimestamps.put(event.containerId(), event.timeInMillis());
    }

    @EventListener
    public void onContainerStarted(ContainerEvent event) {
        if (event.action() == Action.START || event.action() == Action.RESTART) {
            // 시작(사용자/자동) 후 기록 초기화
            manualStopTimestamps.remove(event.containerId());
            restartCounts.remove(event.containerId());
        }
    }

    @EventListener
    public void onContainerDie(ContainerEvent event) {
        if (event.action() != Action.DIE) {
            return;
        }

        String containerId = event.containerId();
        long nowMillis = System.currentTimeMillis();
        Long stoppedAt = manualStopTimestamps.get(containerId);
        boolean manual = stoppedAt != null && (nowMillis - stoppedAt) < TimeUnit.SECONDS.toMillis(5);

        if (manual) {
            manualStopTimestamps.remove(containerId);
            log.info("의도된 종료 감지({}), 자동 복구 스킵", containerId);
            return;
        }

        int currentCount = restartCounts.getOrDefault(containerId, 0);
        if (currentCount >= maxRetryCount) {
            log.warn("{} 컨테이너가 {}회 재시도 후에도 DIE 발생, 더 이상 자동 재시작 하지 않음", containerId, currentCount);
            return;
        }

        restartCounts.put(containerId, currentCount + 1);
        log.info("의도치 않은 DIE 감지({}), {}초 뒤 재시작 예약 (시도: {}/{})",
                containerId, retryDelaySeconds, currentCount + 1, maxRetryCount);

        scheduler.schedule(() -> restartContainer(containerId), retryDelaySeconds, TimeUnit.SECONDS);
    }

    private void scheduleRestartForExistingExited() {
        try {
            Process ps = new ProcessBuilder(
                    "docker", "ps", "-a",
                    "--filter", "status=exited",
                    "--format", "{{.ID}}"
            ).start();

            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(ps.getInputStream()))) {
                String cid;
                while ((cid = r.readLine()) != null) {
                    cid = cid.trim();
                    if (cid.isEmpty()) continue;
                    log.info("기존 exited 컨테이너 {} → {}초 뒤 재시작 예약", cid, retryDelaySeconds);
                    restartCounts.put(cid, 0);
                    String rawCid = cid;
                    scheduler.schedule(() -> restartContainer(rawCid), retryDelaySeconds, TimeUnit.SECONDS);
                }
            }
            ps.waitFor();
        } catch (Exception e) {
            log.error("기존 exited 컨테이너 스캔 중 오류", e);
        }
    }

    private void restartContainer(String containerId) {
        try {
            log.info("스케줄러: {} 재시작 시도 (시도: {}/{})", containerId,
                    restartCounts.getOrDefault(containerId, 0), maxRetryCount);
            Process restart = new ProcessBuilder("docker", "restart", containerId).start();
            int code = restart.waitFor();
            if (code == 0) {
                log.info("{} 재시작 완료 (exitCode={})", containerId, code);
                // 재시작 후 기록 초기화
                manualStopTimestamps.remove(containerId);
                restartCounts.remove(containerId);
            } else {
                log.error("{} 재시작 실패 (exitCode={})", containerId, code);
            }
        } catch (Exception ex) {
            log.error("컨테이너 {} 재시작 중 오류", containerId, ex);
        }
    }

}
