package com.cm2.repository;

import com.cm2.entity.ContainerEvent;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DockerEventRepositoryImpl implements DockerEventRepository {
    private final Map<String, List<ContainerEvent>> logMap = new HashMap<>();

    @Override
    public List<ContainerEvent> findByContainerId(String containerId) {
        return logMap.getOrDefault(containerId, null);
    }

    @Override
    public void save(String containerId, ContainerEvent event) {
        logMap.get(containerId).add(event);
    }

    @Override
    public void clearAll() {
        logMap.clear();
    }

    @Override
    public void createContainerIfNotExists(String containerId) {
        if (!logMap.containsKey(containerId)) {
            logMap.put(containerId, new ArrayList<>());
        }
    }
}