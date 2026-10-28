package com.zodiakk.projectservice.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Aggregated employee task statistics for a period")
public record TaskStatisticsResponseDto(

        @Schema(description = "Employee id")
        UUID employeeId,

        @Schema(description = "Period start date")
        LocalDate periodStart,

        @Schema(description = "Period end date")
        LocalDate periodEnd,

        @Schema(description = "Number of completed tasks")
        long completedTasks,

        @Schema(description = "Completed tasks grouped by task type code")
        Map<String, Long> completedByType,

        @Schema(description = "Completed tasks grouped by complexity")
        Map<String, Long> completedByComplexity,

        @Schema(description = "Number of projects the employee participated in during the period")
        long projectsParticipated
) {
}
