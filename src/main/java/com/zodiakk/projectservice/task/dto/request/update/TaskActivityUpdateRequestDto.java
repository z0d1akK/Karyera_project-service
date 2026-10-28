package com.zodiakk.projectservice.task.dto.request.update;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskActivityUpdateRequestDto {

    @Schema(description = "Action")
    @Size(max = 50, message = ValidationMessages.ACTION_SIZE)
    private String action;

    @Schema(description = "Changes")
    private String changes;
}
