package com.cm2.entity.dto;

import lombok.Builder;

@Builder
public record PortMapping(int internal, int external) {
}
