package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.FcmPlatform;
import com.freshlab.freshdoctor.domain.FcmRegistration;
import com.freshlab.freshdoctor.domain.User;
import com.freshlab.freshdoctor.dto.FcmRegistrationRequest;
import com.freshlab.freshdoctor.exception.FcmRegistrationNotFoundException;
import com.freshlab.freshdoctor.repository.FcmRegistrationRepository;
import com.freshlab.freshdoctor.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FcmRegistrationServiceTest {

    private final FcmRegistrationRepository registrationRepository = mock(FcmRegistrationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FcmRegistrationService service =
            new FcmRegistrationService(registrationRepository, userRepository);

    @Test
    void registersNewDeviceForCurrentUser() {
        User user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(registrationRepository.findByRegistrationKey("token")).thenReturn(Optional.empty());
        when(registrationRepository.save(any(FcmRegistration.class))).thenAnswer(invocation -> {
            FcmRegistration registration = invocation.getArgument(0);
            registration.setId(10L);
            registration.setRegisteredAt(java.time.LocalDateTime.now());
            return registration;
        });

        var response = service.register(1L,
                new FcmRegistrationRequest("token", FcmPlatform.WEB, "Chrome"));

        assertThat(response.registrationId()).isEqualTo(10L);
        assertThat(response.active()).isTrue();
    }

    @Test
    void reassignsExistingBrowserTokenToLatestLoggedInUser() {
        User oldUser = user(1L);
        User currentUser = user(2L);
        FcmRegistration existing = registration(10L, oldUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(currentUser));
        when(registrationRepository.findByRegistrationKey("token")).thenReturn(Optional.of(existing));
        when(registrationRepository.save(existing)).thenReturn(existing);

        service.register(2L, new FcmRegistrationRequest("token", FcmPlatform.WEB, "Edge"));

        assertThat(existing.getUser().getUserId()).isEqualTo(2L);
        assertThat(existing.getDeviceName()).isEqualTo("Edge");
        assertThat(existing.getActive()).isTrue();
    }

    @Test
    void cannotDeactivateAnotherUsersRegistration() {
        when(registrationRepository.findByIdAndUserUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unregister(2L, 10L))
                .isInstanceOf(FcmRegistrationNotFoundException.class);
    }

    private User user(Long id) {
        User user = new User();
        user.setUserId(id);
        return user;
    }

    private FcmRegistration registration(Long id, User user) {
        FcmRegistration registration = new FcmRegistration();
        registration.setId(id);
        registration.setUser(user);
        registration.setRegistrationKey("token");
        registration.setPlatform(FcmPlatform.WEB);
        registration.setActive(true);
        registration.setRegisteredAt(java.time.LocalDateTime.now());
        registration.setLastSeenAt(java.time.LocalDateTime.now());
        return registration;
    }
}
