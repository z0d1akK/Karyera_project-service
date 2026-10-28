package com.zodiakk.projectservice.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBoardResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Board columns ordered by position")
    private List<BoardColumnResponseDto> columns;
}
