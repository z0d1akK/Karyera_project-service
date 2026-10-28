package com.zodiakk.projectservice.internal.controller;

import com.zodiakk.projectservice.internal.dto.ProjectHistoryResponseDto;
import com.zodiakk.projectservice.internal.dto.TaskStatisticsResponseDto;
import com.zodiakk.projectservice.internal.dto.WorkEligibilityResponseDto;
import com.zodiakk.projectservice.internal.service.InternalProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
@Tag(name = "Internal project contracts", description = "Service-to-service project contracts")
public class InternalProjectController {

    private final InternalProjectService internalProjectService;

    @GetMapping("/projects/{projectId}/work-eligibility")
    @Operation(summary = "Check work eligibility",
            description = "Returns project, task and assignment facts for time tracking. "
                    + "Closed projects stay eligible for work dates inside their historical period")
    @ApiResponse(responseCode = "200", description = "Eligibility facts resolved",
            content = @Content(schema = @Schema(implementation = WorkEligibilityResponseDto.class)))
    public ResponseEntity<WorkEligibilityResponseDto> getWorkEligibility(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID projectId,
            @Parameter(description = "Employee identifier", required = true) @RequestParam UUID employeeId,
            @Parameter(description = "Task identifier") @RequestParam(required = false) UUID taskId,
            @Parameter(description = "Work date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate
    ) {
        return ResponseEntity.ok(
                internalProjectService.getWorkEligibility(projectId, employeeId, taskId, workDate)
        );
    }

    @GetMapping("/employees/{employeeId}/project-history")
    @Operation(summary = "Get employee project history",
            description = "Returns project participation periods with roles and project characteristics")
    @ApiResponse(responseCode = "200", description = "Project history resolved",
            content = @Content(schema = @Schema(implementation = ProjectHistoryResponseDto.class)))
    public ResponseEntity<ProjectHistoryResponseDto> getProjectHistory(
            @Parameter(description = "Employee identifier", required = true) @PathVariable UUID employeeId
    ) {
        return ResponseEntity.ok(internalProjectService.getProjectHistory(employeeId));
    }

    @GetMapping("/employees/{employeeId}/task-statistics")
    @Operation(summary = "Get employee task statistics",
            description = "Returns aggregated task statistics for the requested period")
    @ApiResponse(responseCode = "200", description = "Task statistics resolved",
            content = @Content(schema = @Schema(implementation = TaskStatisticsResponseDto.class)))
    public ResponseEntity<TaskStatisticsResponseDto> getTaskStatistics(
            @Parameter(description = "Employee identifier", required = true) @PathVariable UUID employeeId,
            @Parameter(description = "Period start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @Parameter(description = "Period end date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) {
        return ResponseEntity.ok(
                internalProjectService.getTaskStatistics(employeeId, periodStart, periodEnd)
        );
    }
}
