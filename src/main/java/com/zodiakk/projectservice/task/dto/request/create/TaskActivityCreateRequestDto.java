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
public class TaskActivityCreateRequestDto {

    @Schema(description = "Task id")
    @NotNull(message = ValidationMessages.TASK_ID_REQUIRED)
    private UUID taskId;

    @Schema(description = "Action")
    @NotBlank(message = ValidationMessages.ACTION_REQUIRED)
    @Size(max = 50, message = ValidationMessages.ACTION_SIZE)
    private String action;

    @Schema(description = "Changes")
    private String changes;
}
