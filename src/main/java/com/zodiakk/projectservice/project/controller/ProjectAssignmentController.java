package com.zodiakk.projectservice.project.controller;

import com.zodiakk.projectservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.projectservice.project.dto.request.action.ProjectAssignmentEndRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectAssignmentUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectAssignmentResponseDto;
import com.zodiakk.projectservice.project.service.ProjectAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Tag(name = "Project assignments", description = "Operations on project assignments")
public class ProjectAssignmentController {

    private final ProjectAssignmentService projectAssignmentService;

    @PatchMapping("/{id}")
    @Operation(summary = "Update assignment", description = "Updates an existing project assignment")
    @ApiResponse(responseCode = "200", description = "Assignment successfully updated",
            content = @Content(schema = @Schema(implementation = ProjectAssignmentResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Assignment not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectAssignmentResponseDto> update(
            @Parameter(description = "Assignment identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectAssignmentUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectAssignmentService.update(id, request));
    }

    @PostMapping("/{id}/end")
    @Operation(summary = "End assignment", description = "Marks the project assignment as completed")
    @ApiResponse(responseCode = "200", description = "Assignment successfully ended",
            content = @Content(schema = @Schema(implementation = ProjectAssignmentResponseDto.class)))
    public ResponseEntity<ProjectAssignmentResponseDto> end(
            @Parameter(description = "Assignment identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody(required = false) ProjectAssignmentEndRequestDto request
    ) {
        return ResponseEntity.ok(
                projectAssignmentService.end(id, request != null ? request : new ProjectAssignmentEndRequestDto())
        );
    }
}
