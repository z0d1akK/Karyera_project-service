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
public class TaskSkillCreateRequestDto {

    @Schema(description = "Task id")
    private UUID taskId;

    @Schema(description = "Skill id")
    @NotNull(message = ValidationMessages.SKILL_ID_REQUIRED)
    private UUID skillId;
}
