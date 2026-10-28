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
public class ProjectCreateRequestDto {

    @Schema(description = "Code")
    @NotBlank(message = ValidationMessages.CODE_REQUIRED)
    @Size(max = 20, message = ValidationMessages.CODE_SIZE)
    private String code;

    @Schema(description = "Name")
    @NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @Size(max = 200, message = ValidationMessages.NAME_SIZE)
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Start date")
    @NotNull(message = ValidationMessages.START_DATE_REQUIRED)
    private LocalDate startDate;

    @Schema(description = "End date")
    private LocalDate endDate;

    @Schema(description = "Project manager id")
    private UUID projectManagerId;

    @Schema(description = "Delivery manager id")
    private UUID deliveryManagerId;
}
