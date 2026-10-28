package com.zodiakk.projectservice.project.dto.request.update;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMilestoneUpdateRequestDto {

    @Schema(description = "Title")
    @Size(max = 200, message = ValidationMessages.TITLE_SIZE)
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Completed date")
    private LocalDate completedDate;

    @Schema(description = "Status")
    @Size(max = 20, message = ValidationMessages.STATUS_SIZE)
    private String status;
}
