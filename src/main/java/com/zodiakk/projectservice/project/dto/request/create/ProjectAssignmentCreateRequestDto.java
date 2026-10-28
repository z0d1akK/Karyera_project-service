package com.zodiakk.projectservice.project.dto.request.create;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAssignmentCreateRequestDto {

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Employee id")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Project role id")
    @NotNull(message = ValidationMessages.PROJECT_ROLE_ID_REQUIRED)
    private UUID projectRoleId;

    @Schema(description = "Start date")
    @NotNull(message = ValidationMessages.START_DATE_REQUIRED)
    private LocalDate startDate;

    @Schema(description = "End date")
    private LocalDate endDate;

    @Schema(description = "Allocation percent")
    private BigDecimal allocationPercent;
}
