package com.cm2.controller;

import com.cm2.entity.dto.ContainerDetail;
import com.cm2.entity.dto.ContainerInfoResponse;
import com.cm2.service.ContainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService containerService;

    // 모든 컨테이너 목록 조회 API
    @GetMapping
    public ResponseEntity<?> getAllContainers(
            @RequestParam(value = "namespace", required = false) String namespace,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "page", defaultValue = "1") int page) {

        try {
            ContainerInfoResponse response = containerService.getContainerInfo(namespace, status, limit, page);
            if (response.getTotal() == 0)
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 특정 컨테이너의 상세 정보 조회 API
    @GetMapping("/{containerId}")
    public ResponseEntity<?> getContainerDetail(@PathVariable String containerId) {
        try {
            ContainerDetail detail = containerService.getContainerDetail(containerId);
            if (detail == null)
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity<>(detail, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
