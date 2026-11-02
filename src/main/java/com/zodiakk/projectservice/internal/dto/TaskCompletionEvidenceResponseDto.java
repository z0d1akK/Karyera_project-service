package com.zodiakk.projectservice.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Completion evidence for a project task and employee")
public record TaskCompletionEvidenceResponseDto(

        @Schema(description = "Task id")
        UUID taskId,

        @Schema(description = "Employee id")
        UUID employeeId,

        @Schema(description = "Whether the task is completed")
        boolean taskCompleted,

        @Schema(description = "Whether the employee has a verifiable link to the task")
        boolean employeeParticipated,

        @Schema(description = "Task completion timestamp")
        OffsetDateTime completedAt
) {
}
