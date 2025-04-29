package com.cm2.collector.dto;

import lombok.Builder;

@Builder
public record StatusSummary(Indicator indicator,String description) {
}
