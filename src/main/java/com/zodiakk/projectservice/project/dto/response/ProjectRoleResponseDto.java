package com.zodiakk.projectservice.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRoleResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Code")
    private String code;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Active")
    private Boolean active;
}
