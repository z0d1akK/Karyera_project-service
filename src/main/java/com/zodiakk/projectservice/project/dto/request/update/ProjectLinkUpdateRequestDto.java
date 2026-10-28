package com.zodiakk.projectservice.project.dto.request.update;

import com.zodiakk.projectservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLinkUpdateRequestDto {

    @Schema(description = "Label")
    @Size(max = 150, message = ValidationMessages.LABEL_SIZE)
    private String label;

    @Schema(description = "Url")
    @Size(max = 2048, message = ValidationMessages.URL_SIZE)
    private String url;
}
