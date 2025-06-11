package com.cm2.controller;

import com.cm2.collector.ServiceCollector;
import com.cm2.entity.dto.ScaleRequest;
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

    @PostMapping
    public ResponseEntity<String> create(@RequestBody ServiceRequest req) {
        String id = collector.createService(req);
        return ResponseEntity.ok(id);
    }

    @GetMapping
    public ResponseEntity<ServiceListResponse> list() {
        return ResponseEntity.ok(collector.listServices());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> remove(@PathVariable("id") String id) {
        collector.removeService(id);
        return ResponseEntity.ok("Service " + id + " deleted");
    }

    @PutMapping("/{id}/scale")
    public ResponseEntity<String> scale(
            @PathVariable("id") String id,
            @RequestBody ScaleRequest req
    ) {
        collector.scaleService(id, req);
        return ResponseEntity.ok("Service " + id + " scaled to " + req.replicas());
    }
}
