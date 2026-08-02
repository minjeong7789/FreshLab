package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.SignupRequest;
import com.freshlab.freshdoctor.dto.SignupResponse;
import com.freshlab.freshdoctor.dto.LoginRequest;
import com.freshlab.freshdoctor.dto.LoginResponse;
import com.freshlab.freshdoctor.dto.EmailVerificationRequest;
import com.freshlab.freshdoctor.dto.EmailVerificationConfirmRequest;
import com.freshlab.freshdoctor.dto.MessageResponse;
import com.freshlab.freshdoctor.dto.NicknameAvailabilityResponse;
import com.freshlab.freshdoctor.service.AuthService;
import com.freshlab.freshdoctor.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email-verifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse sendEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.sendCode(request.email());
        return new MessageResponse("인증번호를 발송했습니다.");
    }

    @PostMapping("/email-verifications/confirm")
    public MessageResponse confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return new MessageResponse("이메일 인증이 완료되었습니다.");
    }

    @GetMapping("/nicknames/availability")
    public NicknameAvailabilityResponse checkNicknameAvailability(
            @RequestParam
            @NotBlank(message = "닉네임은 필수입니다.")
            @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
            String nickname
    ) {
        return authService.checkNicknameAvailability(nickname);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
