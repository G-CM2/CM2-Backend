package com.cm2.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ActionRequest {
    private String action;

    @Builder
    public ActionRequest(String action) {
        this.action = action;
    }
}
