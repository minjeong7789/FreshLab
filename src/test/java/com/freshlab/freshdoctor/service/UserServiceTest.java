package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.User;
import com.freshlab.freshdoctor.dto.CurrentUserResponse;
import com.freshlab.freshdoctor.repository.UserRepository;
import com.freshlab.freshdoctor.security.InvalidTokenException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    @Test
    void getsCurrentUserProfile() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");
        user.setNickname("민정");
        user.setRegion("서울");
        user.setCreatedAt(LocalDateTime.of(2026, 7, 23, 12, 0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CurrentUserResponse response = userService.getCurrentUser(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("민정");
        assertThat(response.region()).isEqualTo("서울");
    }

    @Test
    void rejectsTokenForMissingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(1L))
                .isInstanceOf(InvalidTokenException.class);
    }
}
