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

    // 컨테이너 리스트 조회
    @GetMapping
    public ResponseEntity<ContainerListResponse> listContainers(
            @RequestParam(value="namespace", required=false) String namespace,
            @RequestParam(value="status",    required=false) String status,
            @RequestParam(value="limit",     defaultValue="20") int limit,
            @RequestParam(value="page",      defaultValue="1")  int page
    ) {
        var list = containerCollector.getContainerInfo(namespace, status, limit, page);
        return list.total() == 0
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(list);
    }

    // 특정 컨테이너 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ContainerDetail> getContainerDetail(@PathVariable("id") String id) {
        var detail = containerCollector.getContainerDetail(id);
        return detail == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(detail);
    }

}
