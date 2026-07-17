package com.freshlab.freshdoctor.dto;

public record NicknameAvailabilityResponse(
        String nickname,
        boolean available
) {
}
