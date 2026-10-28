package com.zodiakk.projectservice.board.dto.request.create;

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
public class ProjectBoardCreateRequestDto {

    @Schema(description = "Project id")
    @NotNull(message = ValidationMessages.PROJECT_ID_REQUIRED)
    private UUID projectId;

    @Schema(description = "Name")
    @NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @Size(max = 100, message = ValidationMessages.NAME_SIZE)
    private String name;
}
