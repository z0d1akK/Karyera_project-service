package com.zodiakk.projectservice.project.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAssignmentFilterDto {

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Project role id")
    private UUID projectRoleId;

    @Schema(description = "Start date from (inclusive)")
    private LocalDate startDateFrom;

    @Schema(description = "Start date to (inclusive)")
    private LocalDate startDateTo;

    @Schema(description = "End date from (inclusive)")
    private LocalDate endDateFrom;

    @Schema(description = "End date to (inclusive)")
    private LocalDate endDateTo;
}
