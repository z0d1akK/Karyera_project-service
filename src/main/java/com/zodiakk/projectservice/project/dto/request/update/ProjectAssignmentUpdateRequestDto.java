package com.zodiakk.projectservice.project.dto.request.update;

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
public class ProjectAssignmentUpdateRequestDto {

    @Schema(description = "Project role id")
    private UUID projectRoleId;

    @Schema(description = "Start date")
    private LocalDate startDate;

    @Schema(description = "End date")
    private LocalDate endDate;

    @Schema(description = "Allocation percent")
    private BigDecimal allocationPercent;
}
