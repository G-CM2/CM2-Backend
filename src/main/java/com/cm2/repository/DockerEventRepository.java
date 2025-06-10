package com.cm2.repository;

import com.cm2.entity.ContainerEvent;
import java.util.List;

public interface DockerEventRepository {
    List<ContainerEvent> findByContainerId(String containerId);
    void save(String containerId, ContainerEvent event);
    void clearAll();
    void createContainerIfNotExists(String containerId);

}