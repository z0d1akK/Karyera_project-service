package com.zodiakk.projectservice.project.dto.response;

import com.zodiakk.projectservice.common.dto.*;
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
public class ProjectResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated at")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Code")
    private String code;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Status")
    private ProjectStatus status;

    @Schema(description = "Start date")
    private LocalDate startDate;

    @Schema(description = "End date")
    private LocalDate endDate;

    @Schema(description = "Project manager id")
    private UUID projectManagerId;

    @Schema(description = "Delivery manager id")
    private UUID deliveryManagerId;

    @Schema(description = "Project manager details")
    private EmployeeReferenceDto projectManager;

    @Schema(description = "Delivery manager details")
    private EmployeeReferenceDto deliveryManager;

    @Schema(description = "Number of project members")
    private Long memberCount;

    @Schema(description = "Number of project tasks")
    private Long taskCount;
}
