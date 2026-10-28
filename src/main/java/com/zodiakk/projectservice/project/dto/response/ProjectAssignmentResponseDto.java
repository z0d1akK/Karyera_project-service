package com.zodiakk.projectservice.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAssignmentResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated at")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Project role id")
    private UUID projectRoleId;

    @Schema(description = "Start date")
    private LocalDate startDate;

    @Schema(description = "End date")
    private LocalDate endDate;

    @Schema(description = "Allocation percent")
    private BigDecimal allocationPercent;

    @Schema(description = "Resolved employee full name")
    private String employeeFullName;

    @Schema(description = "Resolved project role name")
    private String projectRoleName;
}
