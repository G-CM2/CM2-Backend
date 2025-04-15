package com.cm2.entity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ActionResponse {
    private boolean success;
    private String message;
    private String status;

    @Builder
    public ActionResponse(boolean success, String message, String status) {
        this.success = success;
        this.message = message;
        this.status = status;
    }
}

