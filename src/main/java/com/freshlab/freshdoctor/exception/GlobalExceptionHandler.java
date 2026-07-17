package com.freshlab.freshdoctor.exception;

import com.freshlab.freshdoctor.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidRegionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRegion(InvalidRegionException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REGION", exception.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", message));
    }

    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(DuplicateNicknameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_NICKNAME", exception.getMessage()));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(PasswordMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("PASSWORD_MISMATCH", exception.getMessage()));
    }

    @ExceptionHandler(EmailVerificationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleEmailVerificationRequired(
            EmailVerificationRequiredException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("EMAIL_VERIFICATION_REQUIRED", exception.getMessage()));
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ErrorResponse> handleEmailVerification(EmailVerificationException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case "EMAIL_VERIFICATION_TOO_MANY_REQUESTS" -> HttpStatus.TOO_MANY_REQUESTS;
            case "EMAIL_DELIVERY_FAILED" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_EMAIL", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", message));
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItemNotFound(ItemNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ITEM_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvalidPriceDateRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPriceDateRange(InvalidPriceDateRangeException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_PRICE_DATE_RANGE", exception.getMessage()));
    }

    @ExceptionHandler(InvalidPricePeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPricePeriod(InvalidPricePeriodException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_PRICE_PERIOD", exception.getMessage()));
    }

    @ExceptionHandler(RiskScoreNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRiskScoreNotFound(RiskScoreNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RISK_SCORE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(RecommendationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRecommendationNotFound(RecommendationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RECOMMENDATION_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        if (exception.getRequiredType() == java.time.LocalDate.class) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(
                            "INVALID_DATE_FORMAT",
                            "날짜는 yyyy-MM-dd 형식이어야 합니다."
                    ));
        }
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST_PARAMETER", "요청 파라미터 형식이 올바르지 않습니다."));
    }
}
