package com.zodiakk.projectservice.project.dto.request.create;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMilestoneCreateRequestDto {

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Title")
    @NotBlank(message = ValidationMessages.TITLE_REQUIRED)
    @Size(max = 200, message = ValidationMessages.TITLE_SIZE)
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Completed date")
    private LocalDate completedDate;

    @Schema(description = "Status")
    @NotBlank(message = ValidationMessages.STATUS_REQUIRED)
    @Size(max = 20, message = ValidationMessages.STATUS_SIZE)
    private String status;
}
