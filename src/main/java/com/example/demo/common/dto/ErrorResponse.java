package com.example.demo.common.dto;

import java.time.OffsetDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "エラー応答")
public class ErrorResponse {

    @Schema(description = "エラーコード", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "エラーメッセージ", example = "Validation failed")
    private String message;

    @Schema(description = "バリデーションエラー詳細（field -> message）", nullable = true)
    private Map<String, String> errors;

    @Schema(description = "リクエストパス", example = "/dashboard/today")
    private String path;

    @Schema(description = "発生時刻", example = "2026-01-15T10:00:00+09:00")
    private OffsetDateTime timestamp;

    public ErrorResponse(String code, String message, Map<String, String> errors, String path, OffsetDateTime timestamp) {
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.path = path;
        this.timestamp = timestamp;
    }

    // 既存互換（過去のnew ErrorResponse(message, errors)を残したい場合）
    public ErrorResponse(String message, Map<String, String> errors) {
        this("VALIDATION_ERROR", message, errors, null, OffsetDateTime.now());
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public Map<String, String> getErrors() { return errors; }
    public String getPath() { return path; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
