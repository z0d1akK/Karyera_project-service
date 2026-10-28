package com.zodiakk.projectservice.project.dto.request.action;

import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStatusUpdateRequestDto {

    @Schema(description = "New project status", example = "ACTIVE")
    @NotNull(message = ValidationMessages.STATUS_REQUIRED)
    private ProjectStatus status;

    @Schema(description = "Expected entity version for optimistic locking")
    @NotNull(message = ValidationMessages.EXPECTED_VERSION_REQUIRED)
    private Long expectedVersion;
}
