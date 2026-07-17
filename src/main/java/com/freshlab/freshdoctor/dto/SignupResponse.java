package com.freshlab.freshdoctor.dto;

import java.time.LocalDateTime;

public record SignupResponse(
        Long userId,
        String email,
        String nickname,
        String region,
        LocalDateTime createdAt
) {
}
