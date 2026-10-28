package com.zodiakk.projectservice.project.dto.request.update;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRoleUpdateRequestDto {

    @Schema(description = "Code")
    @Size(max = 50, message = ValidationMessages.CODE_SIZE)
    private String code;

    @Schema(description = "Name")
    @Size(max = 100, message = ValidationMessages.NAME_SIZE)
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Active")
    private Boolean active;
}
