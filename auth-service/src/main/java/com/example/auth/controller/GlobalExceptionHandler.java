package com.example.auth.controller;

import java.time.Instant;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.auth.model.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .findFirst().orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request body");
    }

    @ExceptionHandler({org.springframework.web.server.ResponseStatusException.class, DuplicateKeyException.class})
    public ResponseEntity<ApiError> expected(Exception exception) {
        HttpStatus status = exception instanceof DuplicateKeyException
                ? HttpStatus.CONFLICT
                : HttpStatus.valueOf(((org.springframework.web.server.ResponseStatusException) exception)
                        .getStatusCode().value());
        String message;
        if (exception instanceof DuplicateKeyException) {
            message = "Email is already registered";
        } else {
            org.springframework.web.server.ResponseStatusException statusException =
                (org.springframework.web.server.ResponseStatusException) exception;
            message = statusException.getReason() == null
                ? statusException.getStatusCode().toString()
                : statusException.getReason();
        }
        return error(status, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }
}