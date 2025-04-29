package com.cm2.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PortMapping {
    private int internal;
    private int external;

    @Builder
    public PortMapping(int internal, int external) {
        this.internal = internal;
        this.external = external;
    }
}
