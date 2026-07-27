package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.FcmRegistrationRequest;
import com.freshlab.freshdoctor.dto.FcmRegistrationResponse;
import com.freshlab.freshdoctor.security.CurrentUserId;
import com.freshlab.freshdoctor.service.FcmRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/fcm-registrations")
@RequiredArgsConstructor
public class FcmRegistrationController {

    private final FcmRegistrationService registrationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FcmRegistrationResponse register(
            @CurrentUserId Long userId,
            @Valid @RequestBody FcmRegistrationRequest request
    ) {
        return registrationService.register(userId, request);
    }

    @DeleteMapping("/{registrationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(
            @CurrentUserId Long userId,
            @PathVariable Long registrationId
    ) {
        registrationService.unregister(userId, registrationId);
    }
}
