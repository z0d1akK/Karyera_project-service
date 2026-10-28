package com.zodiakk.projectservice.internal.service;

import com.zodiakk.projectservice.internal.dto.ProjectHistoryResponseDto;
import com.zodiakk.projectservice.internal.dto.TaskStatisticsResponseDto;
import com.zodiakk.projectservice.internal.dto.WorkEligibilityResponseDto;

import java.time.LocalDate;
import java.util.UUID;

public interface InternalProjectService {

    WorkEligibilityResponseDto getWorkEligibility(
            UUID projectId,
            UUID employeeId,
            UUID taskId,
            LocalDate workDate
    );

    ProjectHistoryResponseDto getProjectHistory(UUID employeeId);

    TaskStatisticsResponseDto getTaskStatistics(
            UUID employeeId,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
