package com.zodiakk.projectservice.task.dto.request.action;

import com.zodiakk.projectservice.common.enums.TaskRelationType;
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

    @Schema(description = "Identifier of the target task")
    @NotNull(message = ValidationMessages.TARGET_TASK_ID_REQUIRED)
    private UUID targetTaskId;

    @Schema(description = "Relation type", example = "BLOCKS")
    @NotNull(message = ValidationMessages.RELATION_TYPE_REQUIRED)
    private TaskRelationType relationType;
}
