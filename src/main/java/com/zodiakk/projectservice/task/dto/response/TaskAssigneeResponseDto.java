package com.zodiakk.projectservice.task.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssigneeResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Task id")
    private UUID taskId;

    @Schema(description = "Assignment id")
    private UUID assignmentId;

    @Schema(description = "Assigned at")
    private OffsetDateTime assignedAt;

    @Schema(description = "Unassigned at")
    private OffsetDateTime unassignedAt;
}
