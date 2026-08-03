package com.devrick.pos.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API error response.")
public class ErrorResponse {
    @Schema(description = "Timestamp when the error occurred", format = "date-time")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "HTTP status name", example = "NOT_FOUND")
    private String error;

    @Schema(description = "Human-readable error message")
    private String message;

    @Schema(description = "Request path that triggered the error", example = "/api/v1/users/123")
    private String path;

    @Schema(description = "Validation failures keyed by field name")
    private Map<String, String> fieldErrors;
}
