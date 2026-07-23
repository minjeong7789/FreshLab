package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.User;

import java.time.LocalDateTime;

public record CurrentUserResponse(
        Long userId,
        String email,
        String nickname,
        String region,
        LocalDateTime createdAt
) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getRegion(),
                user.getCreatedAt()
        );
    }
}
