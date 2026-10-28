package com.zodiakk.projectservice.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMilestoneResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Completed date")
    private LocalDate completedDate;

    @Schema(description = "Status")
    private String status;
}
