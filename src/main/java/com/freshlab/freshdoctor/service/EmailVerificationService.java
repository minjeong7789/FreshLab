package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.EmailVerification;
import com.freshlab.freshdoctor.exception.DuplicateEmailException;
import com.freshlab.freshdoctor.exception.EmailVerificationException;
import com.freshlab.freshdoctor.exception.EmailVerificationRequiredException;
import com.freshlab.freshdoctor.repository.EmailVerificationRepository;
import com.freshlab.freshdoctor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int CODE_BOUND = 1_000_000;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long CODE_VALID_MINUTES = 5;
    private static final long VERIFIED_VALID_MINUTES = 30;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final Clock clock = Clock.systemUTC();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${MAIL_USERNAME:freshlab7789@gmail.com}")
    private String senderAddress;

    @Transactional
    public void sendCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        verificationRepository.findById(email).ifPresent(existing -> {
            if (existing.getSentAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
                throw new EmailVerificationException("EMAIL_VERIFICATION_TOO_MANY_REQUESTS", "인증번호는 60초 후 다시 요청할 수 있습니다.");
            }
        });

        String code = String.format("%06d", secureRandom.nextInt(CODE_BOUND));
        EmailVerification verification = new EmailVerification();
        verification.setEmail(email);
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setSentAt(now);
        verification.setExpiresAt(now.plusMinutes(CODE_VALID_MINUTES));
        verification.setFailedAttempts(0);
        verificationRepository.save(verification);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(email);
        message.setSubject("[FreshDoctor] 이메일 인증번호");
        message.setText("FreshDoctor 회원가입 인증번호는 " + code + " 입니다.\n인증번호는 5분간 유효합니다.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EmailVerificationException("EMAIL_DELIVERY_FAILED", "인증 이메일 발송에 실패했습니다.");
        }
    }

    @Transactional(noRollbackFor = EmailVerificationException.class)
    public void verifyCode(String rawEmail, String code) {
        String email = normalizeEmail(rawEmail);
        EmailVerification verification = verificationRepository.findById(email)
                .orElseThrow(() -> new EmailVerificationException("INVALID_VERIFICATION_CODE", "인증번호가 올바르지 않습니다."));
        LocalDateTime now = LocalDateTime.now(clock);

        if (verification.getExpiresAt().isBefore(now)) {
            throw new EmailVerificationException("EXPIRED_VERIFICATION_CODE", "인증번호가 만료되었습니다.");
        }
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new EmailVerificationException("VERIFICATION_ATTEMPTS_EXCEEDED", "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.");
        }
        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.setFailedAttempts(verification.getFailedAttempts() + 1);
            throw new EmailVerificationException("INVALID_VERIFICATION_CODE", "인증번호가 올바르지 않습니다.");
        }

        verification.setVerifiedAt(now);
    }

    @Transactional
    public void consumeVerifiedEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        EmailVerification verification = verificationRepository.findById(email)
                .orElseThrow(EmailVerificationRequiredException::new);
        LocalDateTime now = LocalDateTime.now(clock);
        if (verification.getVerifiedAt() == null
                || verification.getVerifiedAt().plusMinutes(VERIFIED_VALID_MINUTES).isBefore(now)) {
            throw new EmailVerificationRequiredException();
        }
        verificationRepository.delete(verification);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
