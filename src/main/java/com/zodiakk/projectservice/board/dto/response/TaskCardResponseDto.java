package com.zodiakk.projectservice.board.dto.response;

import com.zodiakk.projectservice.common.enums.TaskPriority;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCardResponseDto {

    @Schema(description = "Task id")
    private UUID id;

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Task number")
    private Long taskNumber;

    @Schema(description = "Human-readable task key")
    private String key;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Resolved task type code")
    private String type;

    @Schema(description = "Priority")
    private TaskPriority priority;

    @Schema(description = "Column id")
    private UUID columnId;

    @Schema(description = "Board position")
    private BigDecimal boardPosition;

    @Schema(description = "Whether the task is blocked by unresolved dependencies")
    private Boolean blocked;

    @Schema(description = "Identifiers of the project assignments assigned to the task")
    private List<UUID> assigneeIds;
}
