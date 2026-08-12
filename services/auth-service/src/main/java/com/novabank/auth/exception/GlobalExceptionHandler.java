package com.novabank.auth.exception;

import com.novabank.shared.api.ErrorResponse;
import com.novabank.shared.api.FieldViolation;
import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthException.class)
    ResponseEntity<ErrorResponse> handleAuth(AuthException ex, HttpServletRequest request) {
        return build(ex.status(), ex.code(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request, List.of());
    }

    private FieldViolation toViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<FieldViolation> fieldErrors
    ) {
        var body = ErrorResponse.of(
                status.value(),
                code,
                message,
                request.getRequestURI(),
                request.getHeader(Correlation.HEADER_NAME),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
