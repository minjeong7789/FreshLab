package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.User;
import com.freshlab.freshdoctor.dto.SignupRequest;
import com.freshlab.freshdoctor.dto.SignupResponse;
import com.freshlab.freshdoctor.dto.LoginRequest;
import com.freshlab.freshdoctor.dto.LoginResponse;
import com.freshlab.freshdoctor.exception.DuplicateEmailException;
import com.freshlab.freshdoctor.exception.InvalidCredentialsException;
import com.freshlab.freshdoctor.exception.EmailVerificationRequiredException;
import com.freshlab.freshdoctor.exception.DuplicateNicknameException;
import com.freshlab.freshdoctor.exception.PasswordMismatchException;
import com.freshlab.freshdoctor.exception.InvalidRegionException;
import com.freshlab.freshdoctor.repository.UserRepository;
import com.freshlab.freshdoctor.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private EmailVerificationService emailVerificationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, emailVerificationService);
    }

    @Test
    void signupNormalizesEmailAndStoresBcryptPassword() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            user.setCreatedAt(LocalDateTime.of(2026, 7, 17, 12, 0));
            return user;
        });

        SignupResponse response = authService.signup(
                signupRequest(" User@Example.COM ", "fresh-user")
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
        assertThat(savedUser.getNickname()).isEqualTo("fresh-user");
        assertThat(savedUser.getRegion()).isEqualTo("서울");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("fresh-user");
        verify(emailVerificationService).consumeVerifiedEmail("user@example.com");
    }

    @Test
    void signupRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(
                signupRequest("USER@example.com", "fresh-user")
        )).isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void signupRejectsUnverifiedEmail() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        doThrow(new EmailVerificationRequiredException())
                .when(emailVerificationService).consumeVerifiedEmail("user@example.com");

        assertThatThrownBy(() -> authService.signup(
                signupRequest("user@example.com", "fresh-user")
        )).isInstanceOf(EmailVerificationRequiredException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void signupRejectsDuplicateNickname() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByNickname("fresh-user")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(
                signupRequest("user@example.com", "fresh-user")
        )).isInstanceOf(DuplicateNicknameException.class);

        verify(emailVerificationService, never()).consumeVerifiedEmail(any());
    }

    @Test
    void signupRejectsPasswordMismatch() {
        SignupRequest request = new SignupRequest(
                "user@example.com", "password123", "different123", "fresh-user", "서울"
        );

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(PasswordMismatchException.class);

        verify(emailVerificationService, never()).consumeVerifiedEmail(any());
    }

    @Test
    void signupRejectsRegionNotSupportedByKamis() {
        SignupRequest request = new SignupRequest(
                "user@example.com", "password123", "password123", "fresh-user", "뉴욕"
        );

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(InvalidRegionException.class);

        verify(emailVerificationService, never()).consumeVerifiedEmail(any());
    }

    @Test
    void loginReturnsAccessTokenForValidCredentials() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("signed.jwt.token");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(
                new LoginRequest(" USER@example.com ", "password123")
        );

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@example.com", "wrong-password")
        )).isInstanceOf(InvalidCredentialsException.class);

        verify(jwtTokenProvider, never()).createAccessToken(any(User.class));
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("unknown@example.com", "password123")
        )).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void checkNicknameAvailabilityReturnsWhetherNicknameExists() {
        when(userRepository.existsByNickname("fresh-user")).thenReturn(false);

        assertThat(authService.checkNicknameAvailability(" fresh-user ").available()).isTrue();
        assertThat(authService.checkNicknameAvailability(" fresh-user ").nickname())
                .isEqualTo("fresh-user");
    }

    private SignupRequest signupRequest(String email, String nickname) {
        return new SignupRequest(email, "password123", "password123", nickname, " 서울 ");
    }
}
