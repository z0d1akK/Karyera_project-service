package com.zodiakk.projectservice.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLinkResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Label")
    private String label;

    @Schema(description = "Url")
    private String url;
}
