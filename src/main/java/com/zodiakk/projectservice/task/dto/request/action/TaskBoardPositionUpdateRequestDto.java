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
public class TaskBoardPositionUpdateRequestDto {

    @Schema(description = "Identifier of the target board column")
    @NotNull(message = ValidationMessages.TARGET_COLUMN_ID_REQUIRED)
    private UUID targetColumnId;

    @Schema(description = "Identifier of the task that should precede the moved card")
    private UUID beforeTaskId;

    @Schema(description = "Identifier of the task that should follow the moved card")
    private UUID afterTaskId;

    @Schema(description = "Expected task version for optimistic locking")
    @NotNull(message = ValidationMessages.EXPECTED_VERSION_REQUIRED)
    private Long expectedVersion;
}
