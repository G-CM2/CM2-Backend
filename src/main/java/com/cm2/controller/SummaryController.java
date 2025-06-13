package com.cm2.controller;

import com.cm2.collector.DockerSummaryCollector;
import com.cm2.entity.dto.summaryapi.SummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/summary")
@RequiredArgsConstructor
public class SummaryController {
    private final DockerSummaryCollector summaryCollector;

    @GetMapping
    public ResponseEntity<SummaryResponse> getSystemSummary() {
        SummaryResponse resp = summaryCollector.getSystemSummary();
        return ResponseEntity.ok(resp);
    }
}

