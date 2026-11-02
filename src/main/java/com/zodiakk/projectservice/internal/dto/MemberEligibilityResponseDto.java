package com.zodiakk.projectservice.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Project membership eligibility for an employee")
public record MemberEligibilityResponseDto(

        @Schema(description = "Project id")
        UUID projectId,

        @Schema(description = "Employee id")
        UUID employeeId,

        @Schema(description = "Whether the employee may be treated as a project member")
        boolean eligible,

        @Schema(description = "Whether the project is active")
        boolean projectActive,

        @Schema(description = "Whether the assignment is active on the current date")
        boolean assignmentActive,

        @Schema(description = "Resolved assignment id")
        UUID assignmentId,

        @Schema(description = "Assignment start date")
        LocalDate assignmentStartDate,

        @Schema(description = "Assignment end date")
        LocalDate assignmentEndDate,

        @Schema(description = "Reason code when the employee is not eligible")
        String reason
) {
}
