package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.User;
import com.freshlab.freshdoctor.domain.KamisRegion;
import com.freshlab.freshdoctor.dto.SignupRequest;
import com.freshlab.freshdoctor.dto.SignupResponse;
import com.freshlab.freshdoctor.dto.LoginRequest;
import com.freshlab.freshdoctor.dto.LoginResponse;
import com.freshlab.freshdoctor.dto.NicknameAvailabilityResponse;
import com.freshlab.freshdoctor.exception.DuplicateEmailException;
import com.freshlab.freshdoctor.exception.InvalidCredentialsException;
import com.freshlab.freshdoctor.exception.DuplicateNicknameException;
import com.freshlab.freshdoctor.exception.PasswordMismatchException;
import com.freshlab.freshdoctor.exception.InvalidRegionException;
import com.freshlab.freshdoctor.repository.UserRepository;
import com.freshlab.freshdoctor.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        String nickname = request.nickname().trim();
        if (userRepository.existsByNickname(nickname)) {
            throw new DuplicateNicknameException();
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new PasswordMismatchException();
        }
        String region = KamisRegion.findByDisplayName(request.region().trim())
                .map(KamisRegion::getDisplayName)
                .orElseThrow(InvalidRegionException::new);
        emailVerificationService.consumeVerifiedEmail(email);

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(nickname);
        user.setRegion(region);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new SignupResponse(
                    savedUser.getUserId(),
                    savedUser.getEmail(),
                    savedUser.getNickname(),
                    savedUser.getRegion(),
                    savedUser.getCreatedAt()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(
                jwtTokenProvider.createAccessToken(user),
                "Bearer",
                jwtTokenProvider.getExpirationSeconds()
        );
    }

    @Transactional(readOnly = true)
    public NicknameAvailabilityResponse checkNicknameAvailability(String rawNickname) {
        String nickname = rawNickname.trim();
        return new NicknameAvailabilityResponse(
                nickname,
                !userRepository.existsByNickname(nickname)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
