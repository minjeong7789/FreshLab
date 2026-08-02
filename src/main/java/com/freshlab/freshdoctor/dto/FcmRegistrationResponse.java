package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.FcmPlatform;
import com.freshlab.freshdoctor.domain.FcmRegistration;

import java.time.LocalDateTime;

public record FcmRegistrationResponse(
        Long registrationId,
        FcmPlatform platform,
        String deviceName,
        boolean active,
        LocalDateTime registeredAt,
        LocalDateTime lastSeenAt
) {
    public static FcmRegistrationResponse from(FcmRegistration registration) {
        return new FcmRegistrationResponse(
                registration.getId(),
                registration.getPlatform(),
                registration.getDeviceName(),
                Boolean.TRUE.equals(registration.getActive()),
                registration.getRegisteredAt(),
                registration.getLastSeenAt()
        );
    }
}
