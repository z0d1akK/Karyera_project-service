package com.zodiakk.projectservice.task.dto.request.action;

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
public class TaskCommentCreateRequestDto {

    @Schema(description = "Identifier of the commenting employee")
    @NotNull(message = ValidationMessages.AUTHOR_EMPLOYEE_ID_REQUIRED)
    private UUID authorEmployeeId;

    @Schema(description = "Comment text")
    @NotBlank(message = ValidationMessages.CONTENT_REQUIRED)
    @Size(max = 10000, message = ValidationMessages.CONTENT_SIZE)
    private String content;
}
