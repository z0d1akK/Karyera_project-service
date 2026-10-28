package com.zodiakk.projectservice.project.dto.request.filter;

import com.zodiakk.projectservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFilterDto {

    @Schema(description = "Code")
    private String code;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Status")
    private ProjectStatus status;

    @Schema(description = "Project manager id")
    private UUID projectManagerId;

    @Schema(description = "Delivery manager id")
    private UUID deliveryManagerId;

    @Schema(description = "Start date from (inclusive)")
    private LocalDate startDateFrom;

    @Schema(description = "Start date to (inclusive)")
    private LocalDate startDateTo;

    @Schema(description = "End date from (inclusive)")
    private LocalDate endDateFrom;

    @Schema(description = "End date to (inclusive)")
    private LocalDate endDateTo;
}
