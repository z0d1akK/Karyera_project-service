package com.zodiakk.projectservice.internal.dto;

import com.zodiakk.projectservice.common.enums.ProjectStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "A single period of employee participation in a project")
public record ProjectHistoryEntryDto(

        @Schema(description = "Project id")
        UUID projectId,

        @Schema(description = "Project code")
        String projectCode,

        @Schema(description = "Project name")
        String projectName,

        @Schema(description = "Project status")
        ProjectStatus projectStatus,

        @Schema(description = "Project role id")
        UUID projectRoleId,

        @Schema(description = "Project role name")
        String projectRoleName,

        @Schema(description = "Assignment start date")
        LocalDate startDate,

        @Schema(description = "Assignment end date")
        LocalDate endDate,

        @Schema(description = "Allocation percent")
        BigDecimal allocationPercent,

        @Schema(description = "Whether the assignment is still active")
        boolean active
) {
}
