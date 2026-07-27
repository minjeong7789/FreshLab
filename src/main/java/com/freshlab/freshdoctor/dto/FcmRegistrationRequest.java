package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.FcmPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FcmRegistrationRequest(
        @NotBlank(message = "FCM registration key is required.")
        @Size(max = 512, message = "FCM registration key is too long.")
        String registrationKey,

        @NotNull(message = "FCM platform is required.")
        FcmPlatform platform,

        @Size(max = 100, message = "Device name is too long.")
        String deviceName
) {
}
