package com.cm2.collector;

import com.cm2.entity.Action;
import com.cm2.entity.ContainerEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@RequiredArgsConstructor
public class DockerEventCollector implements Runnable {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final AtomicReference<Process> dockerProcessRef = new AtomicReference<>();
    private final Map<String, List<ContainerEvent>> logMap = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Long> manualStopTimestamps = new ConcurrentHashMap<>();


    public List<ContainerEvent> getContainerLogList(String containerId) {
        return logMap.getOrDefault(containerId, null);
    }

    @PostConstruct
    public void init() {

        scheduleRestartForExistingExited();

        // JVM 종료 훅 등록 - 가장 마지막 보험으로 작동
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM 종료 훅 실행 - docker events 프로세스 강제 종료 중...");
            destroyDockerProcess();
        }));

        taskExecutor.execute(this);
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

    @PreDestroy
    public void cleanup() {
        log.info("PreDestroy 호출됨 - docker events 프로세스 종료 중...");
        destroyDockerProcess();
    }

    private void destroyDockerProcess() {
        Process process = dockerProcessRef.get();
        if (process != null) {
            try {
                process.destroyForcibly();
                log.info("docker events 프로세스 강제 종료됨");
            } catch (Exception e) {
                log.error("프로세스 종료 중 오류 발생", e);
            }
        }
    }

    //날짜 체크용
    //하루 지나서 변동 시 맵 지우기
    private int day = 0;

    @Override
    public void run() {
        log.info("Docker 이벤트 모니터링 시작");
        try {
            ProcessBuilder builder = new ProcessBuilder("docker", "events", "--format", "{{.Time}},{{.Type}},{{.Action}},{{.Actor.ID}}");

            Process dockerProcess = builder.start();
            dockerProcessRef.set(dockerProcess); // 프로세스 참조 저장

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(dockerProcess.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    String[] split = line.split(",");

                    String time = split[0];
                    String type = split[1];
                    String action = split[2].toUpperCase();
                    String containerId = split[3];

                    //컨테이너 로그만 수집
                    if (!type.equals("container")) continue;

                    log.info(line);

                    int today = LocalDateTime.now().getDayOfMonth();
                    if (today != day) {
                        day = today;
                        logMap.clear();
                        log.info("메모리 로그 초기화");
                    }

                    // DIE 이벤트 감지 시 5분 후 재시작 스케줄
                    Action actionEnum;
                    try {
                        actionEnum = Action.valueOf(action);
                    } catch (IllegalArgumentException e) {
                        log.debug("무시된 이벤트: {}", action);
                        continue;
                    }

                    long nowMillis = System.currentTimeMillis();

                    // STOP/KILL → 의도적 종료 기록
                    if (actionEnum == Action.STOP || actionEnum == Action.KILL) {
                        manualStopTimestamps.put(containerId, nowMillis);
                        continue;
                    }

                    // DIE 이벤트만 자동 복구 예정
                    if (actionEnum == Action.DIE) {
                        Long stoppedAt = manualStopTimestamps.get(containerId);
                        boolean wasManual = stoppedAt != null && (nowMillis - stoppedAt) < 5000;
                        if (wasManual) {
                            // 의도적 종료로 간주 → 스킵 & 기록 제거
                            manualStopTimestamps.remove(containerId);
                            log.info("의도된 종료 감지 ({}), 재시작 스킵", containerId);
                        } else {
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
                    }

                    ContainerEvent event = new ContainerEvent(Long.parseLong(time), Action.valueOf(action));
                    //없으면 리스트 추가
                    if (!logMap.containsKey(containerId))
                        logMap.put(containerId, new ArrayList<>());

                    //로그 생성
                    logMap.get(containerId).add(event);
                }
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                log.error("Docker 이벤트 모니터링 중 오류 발생", e);
            } else {
                log.info("Docker 이벤트 모니터링이 인터럽트로 중단됨");
            }
        } finally {
            log.info("Docker 이벤트 모니터링 종료");
        }
    }
}
