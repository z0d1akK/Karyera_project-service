package com.zodiakk.projectservice.task.dto.request.update;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssigneeUpdateRequestDto {

    @Schema(description = "Unassigned at")
    private OffsetDateTime unassignedAt;
}
