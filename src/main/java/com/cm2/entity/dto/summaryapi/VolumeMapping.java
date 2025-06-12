package com.cm2.entity.dto.summaryapi;

import lombok.Builder;


@Builder
public record VolumeMapping(String source, String target) {
}
