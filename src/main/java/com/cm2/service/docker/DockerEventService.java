package com.cm2.service.docker;

import com.cm2.entity.Action;
import com.cm2.entity.ContainerEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class DockerEventService implements Runnable {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final AtomicReference<Process> dockerProcessRef = new AtomicReference<>();
    private final Map<String, List<ContainerEvent>> logMap = new HashMap<>();
    private final DockerLogFileService dockerLogFileService;

    public List<ContainerEvent> getContainerLogList(String containerId) {
        return logMap.getOrDefault(containerId, null);
    }

    @PostConstruct
    public void init() {
        // JVM 종료 훅 등록 - 가장 마지막 보험으로 작동
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM 종료 훅 실행 - docker events 프로세스 강제 종료 중...");
            destroyDockerProcess();
        }));

        taskExecutor.execute(this);
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
            ProcessBuilder builder = new ProcessBuilder("docker.exe", "events", "--format", "{{.Time}},{{.Type}},{{.Action}},{{.Actor.ID}}");

            Process dockerProcess = builder.start();
            dockerProcessRef.set(dockerProcess); // 프로세스 참조 저장

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(dockerProcess.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    String[] split = line.split(",");

                    String time = split[0];
                    String type = split[1];
                    String action = split[2];
                    String containerId = split[3];

                    //컨테이너 로그만 수집
                    if (!type.equals("container")) continue;

                    log.info(line);

                    LocalDateTime now = LocalDateTime.now();
                    if (now.getDayOfMonth() != day) {
                        day = now.getDayOfMonth();
                        logMap.clear();
                        log.info("메모리 로그 초기화");
                    }


                    ContainerEvent event = new ContainerEvent(Long.parseLong(time), Action.valueOf(action.toUpperCase()));
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
