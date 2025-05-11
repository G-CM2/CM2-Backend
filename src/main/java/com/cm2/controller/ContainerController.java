package com.cm2.controller;

import com.cm2.collector.DockerContainerCollector;
import com.cm2.entity.dto.ActionRequest;
import com.cm2.entity.dto.ActionResponse;
import com.cm2.entity.dto.ContainerDetail;
import com.cm2.entity.dto.ContainerListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final DockerContainerCollector containerCollector;

    // 모든 컨테이너 목록 조회 API
    @GetMapping
    public ResponseEntity<?> getAllContainers(
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "page", defaultValue = "1") int page) {

        try {
            ContainerListResponse response = containerCollector.getContainerInfo(namespace, status, limit, page);
            if (response.total() == 0)
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    // 특정 컨테이너의 상세 정보 조회 API
    @GetMapping("/{containerId}")
    public ResponseEntity<?> getContainerDetail(@PathVariable String containerId) {
        try {
            ContainerDetail detail = containerCollector.getContainerDetail(containerId);
            if (detail == null)
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity<>(detail, HttpStatus.OK);
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    // 컨테이너 제어 API
    @PostMapping("/{containerId}/action")
    public ResponseEntity<?> performAction(
            @PathVariable String containerId,
            @RequestBody ActionRequest req) {
        try {
            ActionResponse response = containerCollector.controlContainer(containerId, req);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }
}
