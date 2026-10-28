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
public class ProjectMilestoneFilterDto {

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Status")
    private String status;

    @Schema(description = "Due date from (inclusive)")
    private LocalDate dueDateFrom;

    @Schema(description = "Due date to (inclusive)")
    private LocalDate dueDateTo;
}
