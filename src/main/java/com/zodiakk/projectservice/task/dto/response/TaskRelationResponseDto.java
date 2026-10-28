package com.zodiakk.projectservice.task.dto.response;

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
public class TaskRelationResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Source task id")
    private UUID sourceTaskId;

    @Schema(description = "Target task id")
    private UUID targetTaskId;

    @Schema(description = "Relation type")
    private TaskRelationType relationType;

    @Schema(description = "Created by")
    private UUID createdBy;
}
