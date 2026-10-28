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
public class BoardColumnUpdateRequestDto {

    @Schema(description = "Code")
    @Size(max = 30, message = ValidationMessages.CODE_SIZE)
    private String code;

    @Schema(description = "Name")
    @Size(max = 100, message = ValidationMessages.NAME_SIZE)
    private String name;

    @Schema(description = "Position")
    private Integer position;

    @Schema(description = "Is initial")
    private Boolean isInitial;

    @Schema(description = "Is terminal")
    private Boolean isTerminal;
}
