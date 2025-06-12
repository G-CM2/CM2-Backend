package com.cm2.controller;

import com.cm2.collector.ServiceCollector;
import com.cm2.entity.dto.ScaleRequest;
import com.cm2.entity.dto.ServiceDetail;
import com.cm2.entity.dto.ServiceListResponse;
import com.cm2.entity.dto.ServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceCollector collector;

    // 서비스 생성
    @PostMapping
    public ResponseEntity<String> create(@RequestBody ServiceRequest req) {
        String id = collector.createService(req);
        return ResponseEntity.ok(id);
    }

    // 서비스 리스트 조회
    @GetMapping
    public ResponseEntity<ServiceListResponse> list() {
        return ResponseEntity.ok(collector.listServices());
    }

    // 서비스 상세 정보 및 태스크 조회
    @GetMapping("/{id}")
    public ResponseEntity<ServiceDetail> detail(@PathVariable("id") String id) {
        ServiceDetail detail = collector.getServiceDetail(id);
        return ResponseEntity.ok(detail);
    }

    // 서비스 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> remove(@PathVariable("id") String id) {
        collector.removeService(id);
        return ResponseEntity.ok("Service " + id + " deleted");
    }

    // 서비스 스케일링
    @PutMapping("/{id}/scale")
    public ResponseEntity<String> scale(
            @PathVariable("id") String id,
            @RequestBody ScaleRequest req
    ) {
        collector.scaleService(id, req);
        return ResponseEntity.ok("Service " + id + " scaled to " + req.replicas());
    }

    // 롤링 업데이트 실행
    @PostMapping("/{id}/update")
    public ResponseEntity<String> rollingUpdate(@PathVariable("id") String id) {
        String result = collector.rollingUpdateService(id);
        return ResponseEntity.ok(result);
    }
}
