package com.cm2.entity;

public record ContainerEvent(String containerId, Action action, long timeInMillis) {

}
