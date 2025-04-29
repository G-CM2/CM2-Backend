package com.cm2.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EnvironmentVar {
    private String key;
    private String value;

    @Builder
    public EnvironmentVar(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
