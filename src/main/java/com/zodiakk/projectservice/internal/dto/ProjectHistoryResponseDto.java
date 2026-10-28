package com.zodiakk.projectservice.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Employee project participation history")
public record ProjectHistoryResponseDto(

        @Schema(description = "Employee id")
        UUID employeeId,

        @Schema(description = "Project participation periods")
        List<ProjectHistoryEntryDto> projects
) {
}
