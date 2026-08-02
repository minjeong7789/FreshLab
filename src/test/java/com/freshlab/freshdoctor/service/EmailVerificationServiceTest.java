package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.EmailVerification;
import com.freshlab.freshdoctor.exception.EmailVerificationException;
import com.freshlab.freshdoctor.exception.EmailVerificationRequiredException;
import com.freshlab.freshdoctor.repository.EmailVerificationRepository;
import com.freshlab.freshdoctor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository verificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JavaMailSender mailSender;

    private PasswordEncoder passwordEncoder;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new EmailVerificationService(
                verificationRepository,
                userRepository,
                passwordEncoder,
                mailSender
        );
        ReflectionTestUtils.setField(service, "senderAddress", "freshlab7789@gmail.com");
    }

    @Test
    void sendCodeStoresHashAndSendsSixDigitCode() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(verificationRepository.findById("user@example.com")).thenReturn(Optional.empty());

        service.sendCode(" User@example.com ");

        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(verificationRepository).save(verificationCaptor.capture());
        verify(mailSender).send(messageCaptor.capture());

        Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(messageCaptor.getValue().getText());
        assertThat(matcher.find()).isTrue();
        String sentCode = matcher.group(1);
        EmailVerification saved = verificationCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getCodeHash()).isNotEqualTo(sentCode);
        assertThat(passwordEncoder.matches(sentCode, saved.getCodeHash())).isTrue();
    }

    @Test
    void verifyCodeMarksEmailAsVerified() {
        EmailVerification verification = verification("123456");
        when(verificationRepository.findById("user@example.com")).thenReturn(Optional.of(verification));

        service.verifyCode("user@example.com", "123456");

        assertThat(verification.getVerifiedAt()).isNotNull();
    }

    @Test
    void verifyCodeRejectsWrongCodeAndCountsAttempt() {
        EmailVerification verification = verification("123456");
        when(verificationRepository.findById("user@example.com")).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.verifyCode("user@example.com", "000000"))
                .isInstanceOf(EmailVerificationException.class);
        assertThat(verification.getFailedAttempts()).isEqualTo(1);
    }

    @Test
    void consumeVerifiedEmailRejectsUnverifiedEmail() {
        when(verificationRepository.findById("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumeVerifiedEmail("user@example.com"))
                .isInstanceOf(EmailVerificationRequiredException.class);
    }

    private EmailVerification verification(String code) {
        EmailVerification verification = new EmailVerification();
        verification.setEmail("user@example.com");
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setSentAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return verification;
    }
}
