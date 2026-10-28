package com.zodiakk.projectservice.project.controller;

import com.zodiakk.projectservice.project.dto.request.update.ProjectLinkUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectLinkResponseDto;
import com.zodiakk.projectservice.project.service.ProjectLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project-links")
@RequiredArgsConstructor
@Tag(name = "Project links", description = "Operations on project external links")
public class ProjectLinkController {

    private final ProjectLinkService projectLinkService;

    @PatchMapping("/{id}")
    @Operation(summary = "Update project link", description = "Updates an external link of a project")
    @ApiResponse(responseCode = "200", description = "Link successfully updated",
            content = @Content(schema = @Schema(implementation = ProjectLinkResponseDto.class)))
    public ResponseEntity<ProjectLinkResponseDto> update(
            @Parameter(description = "Link identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectLinkUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectLinkService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project link", description = "Deletes an external link of a project")
    @ApiResponse(responseCode = "204", description = "Link successfully deleted")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Link identifier", required = true) @PathVariable UUID id
    ) {
        projectLinkService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
