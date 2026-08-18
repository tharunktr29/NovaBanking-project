package com.novabank.payment.exception;

import com.novabank.shared.api.ErrorResponse;
import com.novabank.shared.api.FieldViolation;
import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentException.class)
    ResponseEntity<ErrorResponse> handlePayment(PaymentException ex, HttpServletRequest request) {
        return error(ex.status(), ex.code(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        var fields = ex.getConstraintViolations().stream()
                .map(error -> new FieldViolation(error.getPropertyPath().toString(), error.getMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, fields);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ErrorResponse> handleOptimisticLock(RuntimeException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "The payment changed while this request was being processed", request, List.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_HEADER", ex.getHeaderName() + " header is required", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request, List.of());
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<FieldViolation> fields
    ) {
        return ResponseEntity.status(status).body(ErrorResponse.of(
                status.value(),
                code,
                message,
                request.getRequestURI(),
                request.getHeader(Correlation.HEADER_NAME),
                fields
        ));
    }
}
