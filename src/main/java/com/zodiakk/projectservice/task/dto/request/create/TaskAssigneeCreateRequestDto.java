package com.zodiakk.projectservice.task.dto.request.create;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssigneeCreateRequestDto {

    @Schema(description = "Task id")
    @NotNull(message = ValidationMessages.TASK_ID_REQUIRED)
    private UUID taskId;

    @Schema(description = "Assignment id")
    @NotNull(message = ValidationMessages.ASSIGNMENT_ID_REQUIRED)
    private UUID assignmentId;
}
