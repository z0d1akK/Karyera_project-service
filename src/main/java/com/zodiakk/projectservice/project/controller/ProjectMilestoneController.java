package com.zodiakk.projectservice.project.controller;

import com.zodiakk.projectservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.projectservice.project.dto.request.update.ProjectMilestoneUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectMilestoneResponseDto;
import com.zodiakk.projectservice.project.service.ProjectMilestoneService;
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
@RequestMapping("/api/v1/milestones")
@RequiredArgsConstructor
@Tag(name = "Project milestones", description = "Operations on project milestones")
public class ProjectMilestoneController {

    private final ProjectMilestoneService projectMilestoneService;

    @PatchMapping("/{id}")
    @Operation(summary = "Update milestone", description = "Updates an existing project milestone")
    @ApiResponse(responseCode = "200", description = "Milestone successfully updated",
            content = @Content(schema = @Schema(implementation = ProjectMilestoneResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Milestone not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectMilestoneResponseDto> update(
            @Parameter(description = "Milestone identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectMilestoneUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectMilestoneService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete milestone", description = "Deletes a project milestone")
    @ApiResponse(responseCode = "204", description = "Milestone successfully deleted")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Milestone identifier", required = true) @PathVariable UUID id
    ) {
        projectMilestoneService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
