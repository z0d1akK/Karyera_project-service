package com.zodiakk.projectservice.project.dto.request.create;

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
public class ProjectLinkCreateRequestDto {

    @Schema(description = "Project id")
    private UUID projectId;

    @Schema(description = "Label")
    @NotBlank(message = ValidationMessages.LABEL_REQUIRED)
    @Size(max = 150, message = ValidationMessages.LABEL_SIZE)
    private String label;

    @Schema(description = "Url")
    @NotBlank(message = ValidationMessages.URL_REQUIRED)
    @Size(max = 2048, message = ValidationMessages.URL_SIZE)
    private String url;
}
