package com.zodiakk.projectservice.task.dto.request.create;

import com.zodiakk.projectservice.common.enums.*;
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
public class TaskRelationCreateRequestDto {

    @Schema(description = "Source task id")
    @NotNull(message = ValidationMessages.SOURCE_TASK_ID_REQUIRED)
    private UUID sourceTaskId;

    @Schema(description = "Target task id")
    @NotNull(message = ValidationMessages.TARGET_TASK_ID_REQUIRED)
    private UUID targetTaskId;

    @Schema(description = "Relation type")
    @NotNull(message = ValidationMessages.RELATION_TYPE_REQUIRED)
    private TaskRelationType relationType;
}
