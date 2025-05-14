package com.cm2.controller;

import com.cm2.entity.ContainerEvent;
import com.cm2.repository.DockerEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/timeline")
public class TimelineController {

    private final DockerEventRepository eventRepository;

    @GetMapping("/{containerId}")
    public ResponseEntity<List<ContainerEvent>> getTimeline(
            @PathVariable String containerId
    ) {
        List<ContainerEvent> resultList = eventRepository.findByContainerId(containerId);
        return resultList == null ?
                ResponseEntity.badRequest().body(null) :
                ResponseEntity.ok(resultList);
    }

}
