package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.FcmRegistration;
import com.freshlab.freshdoctor.domain.User;
import com.freshlab.freshdoctor.dto.FcmRegistrationRequest;
import com.freshlab.freshdoctor.dto.FcmRegistrationResponse;
import com.freshlab.freshdoctor.exception.FcmRegistrationNotFoundException;
import com.freshlab.freshdoctor.repository.FcmRegistrationRepository;
import com.freshlab.freshdoctor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmRegistrationService {

    private final FcmRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @Transactional
    public FcmRegistrationResponse register(Long userId, FcmRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        FcmRegistration registration = registrationRepository.findByRegistrationKey(request.registrationKey())
                .orElseGet(() -> {
                    FcmRegistration created = new FcmRegistration();
                    created.setRegistrationKey(request.registrationKey());
                    return created;
                });
        registration.activateFor(user, request.platform(), normalizeDeviceName(request.deviceName()));
        return FcmRegistrationResponse.from(registrationRepository.save(registration));
    }

    @Transactional
    public void unregister(Long userId, Long registrationId) {
        FcmRegistration registration = registrationRepository.findByIdAndUserUserId(registrationId, userId)
                .orElseThrow(FcmRegistrationNotFoundException::new);
        registration.deactivate();
    }

    private String normalizeDeviceName(String deviceName) {
        return deviceName == null || deviceName.isBlank() ? null : deviceName.trim();
    }
}
