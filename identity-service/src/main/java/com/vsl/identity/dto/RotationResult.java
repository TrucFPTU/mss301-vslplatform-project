package com.vsl.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RotationResult {
    private final Long userId;
    private final String newRawToken;
}
