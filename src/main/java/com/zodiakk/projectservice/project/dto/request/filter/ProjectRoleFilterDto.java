package com.zodiakk.projectservice.project.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRoleFilterDto {

    @Schema(description = "Code")
    private String code;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Active")
    private Boolean active;
}
