package com.zodiakk.projectservice.task.dto.request.action;

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
public class TaskParentUpdateRequestDto {

    @Schema(description = "New parent task identifier; omit to detach the task from its parent")
    private UUID parentTaskId;

    @Schema(description = "Expected task version for optimistic locking")
    @NotNull(message = ValidationMessages.EXPECTED_VERSION_REQUIRED)
    private Long expectedVersion;
}
