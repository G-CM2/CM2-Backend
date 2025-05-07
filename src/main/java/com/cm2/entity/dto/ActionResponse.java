package com.cm2.entity.dto;

import lombok.Builder;

@Builder
public record ActionResponse(boolean success, String message, String status) {
}

