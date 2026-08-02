package com.freshlab.freshdoctor.exception;

public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }
}
