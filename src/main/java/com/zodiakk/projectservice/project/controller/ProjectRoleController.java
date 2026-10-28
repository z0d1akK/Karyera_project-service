package com.zodiakk.projectservice.project.controller;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.projectservice.common.dto.response.ValidationErrorResponse;
import com.zodiakk.projectservice.project.dto.request.create.ProjectRoleCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectRoleFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectRoleUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectRoleResponseDto;
import com.zodiakk.projectservice.project.service.ProjectRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project-roles")
@RequiredArgsConstructor
@Tag(name = "Project roles", description = "Project role catalog")
public class ProjectRoleController {

    private final ProjectRoleService projectRoleService;

    @GetMapping
    @Operation(summary = "Get project roles", description = "Returns a paginated list of project roles")
    @ApiResponse(responseCode = "200", description = "Project roles successfully retrieved")
    public ResponseEntity<Page<ProjectRoleResponseDto>> findAll(
            @Valid ProjectRoleFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(projectRoleService.findAll(filter, pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project role", description = "Returns a project role by identifier")
    @ApiResponse(responseCode = "200", description = "Project role found",
            content = @Content(schema = @Schema(implementation = ProjectRoleResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Project role not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectRoleResponseDto> getById(
            @Parameter(description = "Project role identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(projectRoleService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create project role", description = "Creates a project role")
    @ApiResponse(responseCode = "201", description = "Project role successfully created",
            content = @Content(schema = @Schema(implementation = ProjectRoleResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Project role code already exists",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectRoleResponseDto> create(
            @Valid @RequestBody ProjectRoleCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectRoleService.create(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update project role", description = "Updates a project role")
    @ApiResponse(responseCode = "200", description = "Project role successfully updated",
            content = @Content(schema = @Schema(implementation = ProjectRoleResponseDto.class)))
    public ResponseEntity<ProjectRoleResponseDto> update(
            @Parameter(description = "Project role identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectRoleUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectRoleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project role", description = "Deletes a project role that is not used by assignments")
    @ApiResponse(responseCode = "204", description = "Project role successfully deleted")
    @ApiResponse(responseCode = "409", description = "Project role is in use",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Project role identifier", required = true) @PathVariable UUID id
    ) {
        projectRoleService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
