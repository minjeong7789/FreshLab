package com.freshlab.freshdoctor.exception;

import com.freshlab.freshdoctor.dto.ErrorResponse;
import com.freshlab.freshdoctor.security.InvalidTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final String INVALID_REQUEST_MESSAGE = "The request is invalid.";

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAlertNotFound(AlertNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateWatchItemException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateWatchItem(DuplicateWatchItemException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_WATCH_ITEM", ex.getMessage(), request);
    }

    @ExceptionHandler(WatchItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWatchItemNotFound(WatchItemNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "WATCH_ITEM_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedWatchItemException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedWatchItem(UnsupportedWatchItemException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "UNSUPPORTED_WATCH_ITEM", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidRegionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRegion(InvalidRegionException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REGION", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(DuplicateNicknameException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", ex.getMessage(), request);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(PasswordMismatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", ex.getMessage(), request);
    }

    @ExceptionHandler(EmailVerificationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleEmailVerificationRequired(EmailVerificationRequiredException ex,
                                                                         HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "EMAIL_VERIFICATION_REQUIRED", ex.getMessage(), request);
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ErrorResponse> handleEmailVerification(EmailVerificationException ex, HttpServletRequest request) {
        HttpStatus status = switch (ex.getCode()) {
            case "EMAIL_VERIFICATION_TOO_MANY_REQUESTS" -> HttpStatus.TOO_MANY_REQUESTS;
            case "EMAIL_DELIVERY_FAILED" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_REQUEST;
        };
        return error(status, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), request);
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItemNotFound(ItemNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPriceDateRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPriceDateRange(InvalidPriceDateRangeException ex,
                                                                     HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PRICE_DATE_RANGE", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPricePeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPricePeriod(InvalidPricePeriodException ex,
                                                                  HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PRICE_PERIOD", ex.getMessage(), request);
    }

    @ExceptionHandler(RiskScoreNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRiskScoreNotFound(RiskScoreNotFoundException ex,
                                                                 HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RISK_SCORE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(RecommendationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRecommendationNotFound(RecommendationNotFoundException ex,
                                                                       HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RECOMMENDATION_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientCalculationDataException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientData(InsufficientCalculationDataException ex,
                                                                HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_DATA", ex.getMessage(), request);
    }

    @ExceptionHandler({ExternalApiException.class, WebClientRequestException.class, WebClientResponseException.class})
    public ResponseEntity<ErrorResponse> handleExternalApi(Exception ex, HttpServletRequest request) {
        log.warn("External API failure. path={}, type={}", request.getRequestURI(), ex.getClass().getSimpleName());
        return error(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", "An external API request failed.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage()).orElse(INVALID_REQUEST_MESSAGE);
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .findFirst().map(violation -> violation.getMessage()).orElse(INVALID_REQUEST_MESSAGE);
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", INVALID_REQUEST_MESSAGE, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        if (ex.getRequiredType() == LocalDate.class) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_DATE_FORMAT",
                    "Date values must use yyyy-MM-dd format.", request);
        }
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_PARAMETER",
                "A request parameter has an invalid format.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                              HttpServletRequest request) {
        log.warn("Database constraint violation. path={}", request.getRequestURI());
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected server error. path={}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred.", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message,
                                                HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                code,
                message == null || message.isBlank() ? status.getReasonPhrase() : message,
                OffsetDateTime.now(SEOUL_ZONE),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
