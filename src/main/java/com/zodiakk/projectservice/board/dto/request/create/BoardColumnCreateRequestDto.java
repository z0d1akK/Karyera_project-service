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
public class BoardColumnCreateRequestDto {

    @Schema(description = "Board id")
    private UUID boardId;

    @Schema(description = "Code")
    @NotBlank(message = ValidationMessages.CODE_REQUIRED)
    @Size(max = 30, message = ValidationMessages.CODE_SIZE)
    private String code;

    @Schema(description = "Name")
    @NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @Size(max = 100, message = ValidationMessages.NAME_SIZE)
    private String name;

    @Schema(description = "Position")
    @NotNull(message = ValidationMessages.POSITION_REQUIRED)
    private Integer position;

    @Schema(description = "Is initial")
    @NotNull(message = ValidationMessages.IS_INITIAL_REQUIRED)
    private Boolean isInitial;

    @Schema(description = "Is terminal")
    @NotNull(message = ValidationMessages.IS_TERMINAL_REQUIRED)
    private Boolean isTerminal;
}
