package com.cm2.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VolumeMapping {
    private String source;
    private String target;

    @Builder
    public VolumeMapping(String source, String target) {
        this.source = source;
        this.target = target;
    }
}
