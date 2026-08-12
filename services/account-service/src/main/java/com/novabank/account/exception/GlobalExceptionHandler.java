package com.novabank.account.exception;

import com.novabank.shared.api.ErrorResponse;
import com.novabank.shared.api.FieldViolation;
import com.novabank.shared.correlation.Correlation;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccountException.class)
    ResponseEntity<ErrorResponse> handleAccount(AccountException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status()).body(error(exception.status(), exception.code(), exception.getMessage(), request, List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, fieldErrors));
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    ResponseEntity<ErrorResponse> handleOptimisticLock(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "The record was updated by another request. Reload and try again.", request, List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request contains invalid account data", request, List.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneric(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected account service error", request, List.of()));
    }

    private ErrorResponse error(HttpStatus status, String code, String message, HttpServletRequest request, List<FieldViolation> fieldErrors) {
        return ErrorResponse.of(status.value(), code, message, request.getRequestURI(), request.getHeader(Correlation.HEADER_NAME), fieldErrors);
    }
}
