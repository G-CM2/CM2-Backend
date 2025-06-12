package com.cm2.entity.dto.summaryapi;

import lombok.Builder;

@Builder
public record ActionRequest(
        String action,
        String image,
        String networkMode
) { }

