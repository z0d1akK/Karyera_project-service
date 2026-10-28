package com.zodiakk.projectservice.task.dto.request.update;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCommentUpdateRequestDto {

    @Schema(description = "Content")
    private String content;
}
