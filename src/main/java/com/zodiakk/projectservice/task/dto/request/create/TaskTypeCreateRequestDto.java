package com.zodiakk.projectservice.task.dto.request.create;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTypeCreateRequestDto {

    @Schema(description = "Code")
    @NotBlank(message = ValidationMessages.CODE_REQUIRED)
    @Size(max = 30, message = ValidationMessages.CODE_SIZE)
    private String code;

    @Schema(description = "Name")
    @NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @Size(max = 100, message = ValidationMessages.NAME_SIZE)
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Active")
    private Boolean active;
}
