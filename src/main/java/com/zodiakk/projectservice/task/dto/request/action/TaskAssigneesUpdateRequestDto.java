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
public class TaskAssigneesUpdateRequestDto {

    @Schema(description = "Identifiers of the project assignments that should be assigned to the task")
    @NotNull(message = ValidationMessages.ASSIGNEE_IDS_REQUIRED)
    private List<UUID> assigneeIds;

    @Schema(description = "Expected task version for optimistic locking")
    @NotNull(message = ValidationMessages.EXPECTED_VERSION_REQUIRED)
    private Long expectedVersion;
}
