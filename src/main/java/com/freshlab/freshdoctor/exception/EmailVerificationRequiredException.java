package com.freshlab.freshdoctor.exception;

public class EmailVerificationRequiredException extends RuntimeException {

    public EmailVerificationRequiredException() {
        super("이메일 인증이 필요합니다.");
    }
}
