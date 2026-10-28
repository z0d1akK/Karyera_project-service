package com.zodiakk.projectservice.task.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSkillResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Task id")
    private UUID taskId;

    @Schema(description = "Skill id")
    private UUID skillId;
}
