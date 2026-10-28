package com.zodiakk.projectservice.board.dto.request.update;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBoardUpdateRequestDto {

    @Schema(description = "Name")
    @Size(max = 100, message = ValidationMessages.NAME_SIZE)
    private String name;
}
