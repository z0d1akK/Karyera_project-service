package com.zodiakk.projectservice.task.dto.request.create;

import com.zodiakk.projectservice.common.enums.*;
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
public class ProjectTaskCreateRequestDto {

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Title")
    @NotBlank(message = ValidationMessages.TITLE_REQUIRED)
    @Size(max = 255, message = ValidationMessages.TITLE_SIZE)
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Task type id")
    @NotNull(message = ValidationMessages.TASK_TYPE_ID_REQUIRED)
    private UUID taskTypeId;

    @Schema(description = "Column id")
    @NotNull(message = ValidationMessages.COLUMN_ID_REQUIRED)
    private UUID columnId;

    @Schema(description = "Parent task id")
    private UUID parentTaskId;

    @Schema(description = "Priority")
    @NotNull(message = ValidationMessages.PRIORITY_REQUIRED)
    private TaskPriority priority;

    @Schema(description = "Complexity")
    private TaskComplexity complexity;

    @Schema(description = "Story points")
    private BigDecimal storyPoints;

    @Schema(description = "Estimated minutes")
    private Integer estimatedMinutes;

    @Schema(description = "Due date")
    private LocalDate dueDate;
}
