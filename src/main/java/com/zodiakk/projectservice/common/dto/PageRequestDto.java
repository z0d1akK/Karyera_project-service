package com.zodiakk.projectservice.common.dto;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRequestDto {

    @Schema(description = "Page number", example = "0", defaultValue = "0")
    @Min(value = 0, message = ValidationMessages.PAGE_NUMBER_MIN)
    @Builder.Default
    private int page = 0;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Min(value = 1, message = ValidationMessages.PAGE_MIN_SIZE)
    @Max(value = 100, message = ValidationMessages.PAGE_MAX_SIZE)
    @Builder.Default
    private int size = 20;

    @Schema(description = "Field used for sorting", example = "name", defaultValue = "name")
    @Builder.Default
    private String sortBy = "name";

    @Schema(description = "Sort direction", example = "ASC", defaultValue = "ASC")
    @Builder.Default
    private SortDirection sortDirection = SortDirection.ASC;
}
