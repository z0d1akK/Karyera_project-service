package com.zodiakk.projectservice.common.dto.field;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Validation field error")
public class ValidationErrorField {

    @Schema(example = "title")
    private String field;

    @Schema(example = "Title is required")
    private String message;
}
