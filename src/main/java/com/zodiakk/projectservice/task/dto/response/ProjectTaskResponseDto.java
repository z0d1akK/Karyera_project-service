package com.zodiakk.projectservice.task.dto.response;

import com.zodiakk.projectservice.common.enums.*;

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
public class ProjectTaskResponseDto {

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

    @Schema(description = "Task number")
    private Long taskNumber;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Task type id")
    private UUID taskTypeId;

    @Schema(description = "Column id")
    private UUID columnId;

    @Schema(description = "Parent task id")
    private UUID parentTaskId;

    @Schema(description = "Priority")
    private TaskPriority priority;

    @Schema(description = "Complexity")
    private TaskComplexity complexity;

    @Schema(description = "Story points")
    private BigDecimal storyPoints;

    @Schema(description = "Estimated minutes")
    private Integer estimatedMinutes;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Completed at")
    private OffsetDateTime completedAt;

    @Schema(description = "Board position")
    private BigDecimal boardPosition;

    @Schema(description = "Reporter employee id")
    private UUID reporterEmployeeId;

    @Schema(description = "Archived at")
    private OffsetDateTime archivedAt;

    @Schema(description = "Human-readable task key")
    private String key;

    @Schema(description = "Resolved task type code")
    private String type;

    @Schema(description = "Resolved board column code")
    private String status;

    @Schema(description = "Active task assignees")
    private List<TaskAssigneeResponseDto> assignees;

    @Schema(description = "Number of child tasks")
    private Long childrenCount;

    @Schema(description = "Number of completed child tasks")
    private Long completedChildrenCount;

    @Schema(description = "Whether the task is blocked by unresolved dependencies")
    private Boolean blocked;
}
