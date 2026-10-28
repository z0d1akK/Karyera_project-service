package com.zodiakk.projectservice.task.dto.request.filter;

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
public class ProjectTaskFilterDto {

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Column id")
    private UUID columnId;

    @Schema(description = "Task type id")
    private UUID taskTypeId;

    @Schema(description = "Parent task id")
    private UUID parentTaskId;

    @Schema(description = "Priority")
    private TaskPriority priority;

    @Schema(description = "Complexity")
    private TaskComplexity complexity;

    @Schema(description = "Task number")
    private Long taskNumber;

    @Schema(description = "Whether the task is archived")
    private Boolean archived;

    @Schema(description = "Due date from (inclusive)")
    private LocalDate dueDateFrom;

    @Schema(description = "Due date to (inclusive)")
    private LocalDate dueDateTo;

    @Schema(description = "Case-insensitive search over title and description")
    private String search;

    @Schema(description = "Active assignment identifier")
    private UUID assigneeAssignmentId;
}
