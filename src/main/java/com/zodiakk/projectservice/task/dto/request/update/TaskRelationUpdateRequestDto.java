package com.zodiakk.projectservice.task.dto.request.update;

import com.zodiakk.projectservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRelationUpdateRequestDto {

    @Schema(description = "Target task id")
    private UUID targetTaskId;

    @Schema(description = "Relation type")
    private TaskRelationType relationType;
}
