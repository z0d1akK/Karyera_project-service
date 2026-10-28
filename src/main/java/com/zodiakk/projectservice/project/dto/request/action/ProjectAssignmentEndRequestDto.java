package com.zodiakk.projectservice.project.dto.request.action;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAssignmentEndRequestDto {

    @Schema(description = "Assignment end date")
    private LocalDate endDate;

    @Schema(description = "Optional comment explaining the completion")
    @Size(max = 1000, message = ValidationMessages.COMMENT_SIZE)
    private String comment;

    @Schema(description = "Expected entity version for optimistic locking")
    private Long expectedVersion;
}
