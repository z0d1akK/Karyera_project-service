package com.zodiakk.projectservice.common.dto.response;

import com.zodiakk.projectservice.common.dto.field.ValidationErrorField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Validation error response")
public class ValidationErrorResponse {

    @Schema(example = "400")
    private int status;

    @Schema(example = "Bad Request")
    private String error;

    private List<ValidationErrorField> errors;

    @Schema(example = "2026-05-16T15:30:00")
    private LocalDateTime timestamp;
}
