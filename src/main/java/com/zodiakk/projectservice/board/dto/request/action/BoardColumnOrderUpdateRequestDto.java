package com.zodiakk.projectservice.board.dto.request.action;

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
public class BoardColumnOrderUpdateRequestDto {

    @Schema(description = "Ordered board column identifiers")
    @NotEmpty(message = ValidationMessages.COLUMN_IDS_NOT_EMPTY)
    private List<UUID> columnIds;

    @Schema(description = "Expected entity version for optimistic locking")
    private Long expectedVersion;
}
