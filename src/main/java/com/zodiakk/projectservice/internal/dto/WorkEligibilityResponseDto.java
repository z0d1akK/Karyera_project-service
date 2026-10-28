package com.zodiakk.projectservice.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Eligibility facts for recording time against a project task")
public record WorkEligibilityResponseDto(

        @Schema(description = "Whether all eligibility checks passed")
        boolean eligible,

        @Schema(description = "Requested project identifier")
        UUID projectId,

        @Schema(description = "Requested task identifier, null when eligibility was checked without a task")
        UUID taskId,

        @Schema(description = "Requested employee identifier")
        UUID employeeId,

        @Schema(description = "Date the work was performed")
        LocalDate workDate,

        @Schema(description = "Project assignment active on the work date, null when no assignment matches")
        UUID projectAssignmentId,

        @Schema(description = "Whether the project is in the ACTIVE status right now")
        boolean projectActive,

        @Schema(description = "Whether the task belongs to the requested project, null when no task was requested")
        Boolean taskBelongsToProject,

        @Schema(description = "Whether the employee assignment is active on the requested date")
        boolean assignmentActiveOnDate,

        @Schema(description = "Whether the task is archived, null when no task was requested")
        Boolean taskArchived,

        @Schema(description = "Reason of the negative eligibility decision, null when eligible")
        WorkEligibilityReasonCode reasonCode
) {
}
