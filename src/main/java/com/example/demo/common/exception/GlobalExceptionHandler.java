package com.example.demo.common.exception;

import com.example.demo.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // =========================
        // 400: Bean Validation
        // =========================
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationError(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

                return ResponseEntity.badRequest().body(
                                new ErrorResponse(
                                                "VALIDATION_ERROR",
                                                "Validation failed",
                                                errors,
                                                request.getRequestURI(),
                                                OffsetDateTime.now()));
        }

        // =========================
        // 400: 型変換失敗
        // =========================
        @ExceptionHandler({
                        MethodArgumentTypeMismatchException.class,
                        ConversionFailedException.class
        })
        public ResponseEntity<ErrorResponse> handleTypeMismatch(
                        Exception ex,
                        HttpServletRequest request) {

                Map<String, String> errors = new HashMap<>();
                errors.put("date", "must be in yyyy-MM-dd format");

                return ResponseEntity.badRequest().body(
                                new ErrorResponse(
                                                "VALIDATION_ERROR",
                                                "Validation failed",
                                                errors,
                                                request.getRequestURI(),
                                                OffsetDateTime.now()));
        }

        // =========================
        // ResponseStatusExceptionはそのまま尊重
        // =========================
        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ErrorResponse> handleResponseStatus(
                        ResponseStatusException ex,
                        HttpServletRequest request) {

                return ResponseEntity.status(ex.getStatusCode()).body(
                                new ErrorResponse(
                                                ex.getStatusCode().value() >= 500 ? "INTERNAL_ERROR"
                                                                : "VALIDATION_ERROR",
                                                ex.getReason(),
                                                null,
                                                request.getRequestURI(),
                                                OffsetDateTime.now()));
        }

        // =========================
        // 500: その他 RuntimeException
        // =========================
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponse> handleRuntimeException(
                        RuntimeException ex,
                        HttpServletRequest request) {

                return ResponseEntity.internalServerError().body(
                                new ErrorResponse(
                                                "INTERNAL_ERROR",
                                                "Internal Server Error",
                                                null,
                                                request.getRequestURI(),
                                                OffsetDateTime.now()));
        }

        // =========================
        // 最後の砦
        // =========================
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleUnexpectedException(
                        Exception ex,
                        HttpServletRequest request) {

                return ResponseEntity.internalServerError().body(
                                new ErrorResponse(
                                                "INTERNAL_ERROR",
                                                "Internal Server Error",
                                                null,
                                                request.getRequestURI(),
                                                OffsetDateTime.now()));
        }
}
