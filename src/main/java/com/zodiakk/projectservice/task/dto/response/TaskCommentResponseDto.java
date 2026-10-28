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
public class TaskCommentResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated at")
    private OffsetDateTime updatedAt;

    @Schema(description = "Task id")
    private UUID taskId;

    @Schema(description = "Author employee id")
    private UUID authorEmployeeId;

    @Schema(description = "Content")
    private String content;
}
