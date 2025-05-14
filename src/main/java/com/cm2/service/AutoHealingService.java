package com.cm2.service;

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

    private final Map<String, Long> manualStopTimestamps = new ConcurrentHashMap<>();
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
    public void onContainerDie(ContainerEvent event) {
        // DIE 이벤트 감지 시 5분 후 재시작 스케줄
        Action action = event.action();
        if (action != Action.DIE) return;

        String containerId = event.containerId();
        long nowMillis = System.currentTimeMillis();

        // DIE 이벤트만 자동 복구 예정
        Long stoppedAt = manualStopTimestamps.get(containerId);
        boolean wasManual = stoppedAt != null && (nowMillis - stoppedAt) < 5000;

        if (wasManual) {
            manualStopTimestamps.remove(containerId);
            log.info("의도된 종료 감지 ({}), 재시작 스킵", containerId);
            return;
        }

        // 의도치 않은 종료 → 5분 후 재시작 예약
        log.info("의도치 않은 DIE 감지 ({}), 5분 뒤 재시작 예약", containerId);
        scheduler.schedule(() -> {
            try {
                log.info("5분 경과, {} 재시작 시도", containerId);
                Process restart = new ProcessBuilder("docker", "restart", containerId).start();
                int code = restart.waitFor();
                log.info("{} 재시작 완료 (exitCode={})", containerId, code);
            } catch (Exception ex) {
                log.error("컨테이너 {} 재시작 중 오류", containerId, ex);
            }
        }, 5, TimeUnit.MINUTES);
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
                String rawCid;
                while ((rawCid = r.readLine()) != null) {
                    String cid = rawCid.trim();
                    if (cid.isEmpty()) continue;
                    log.info("기존 exited 컨테이너 {} → 5분 뒤 재시작 예약", cid);
                    final String containerId = cid;

                    scheduler.schedule(() -> restartContainer(containerId), 5, TimeUnit.MINUTES);
                }
            }
            ps.waitFor();
        } catch (Exception e) {
            log.error("기존 exited 컨테이너 스캔 중 오류", e);
        }
    }

    private void restartContainer(String containerId) {
        try {
            log.info("스케줄러: {} 재시작 시도", containerId);
            Process restart = new ProcessBuilder("docker", "restart", containerId).start();
            int code = restart.waitFor();
            log.info("{} 재시작 완료 (exitCode={})", containerId, code);
        } catch (Exception ex) {
            log.error("컨테이너 {} 재시작 중 오류", containerId, ex);
        }
    }

}
