package com.zodiakk.projectservice.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardColumnResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Board id")
    private UUID boardId;

    @Schema(description = "Code")
    private String code;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Position")
    private Integer position;

    @Schema(description = "Is initial")
    private Boolean isInitial;

    @Schema(description = "Is terminal")
    private Boolean isTerminal;

    @Schema(description = "Number of tasks in the column")
    private Long taskCount;

    @Schema(description = "Task cards in the column ordered by board position")
    private List<TaskCardResponseDto> tasks;
}
