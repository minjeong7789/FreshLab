package com.freshlab.freshdoctor.exception;

import com.freshlab.freshdoctor.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
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
