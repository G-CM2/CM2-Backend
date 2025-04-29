package com.cm2.collector.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class StatsResult {
    public double cpuUsage;
    public int memoryUsage;
}
