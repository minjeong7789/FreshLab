package com.freshlab.freshdoctor.security;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("유효한 인증 토큰이 필요합니다.");
    }
}
