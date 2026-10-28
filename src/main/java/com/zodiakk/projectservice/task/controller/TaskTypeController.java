package com.zodiakk.projectservice.task.controller;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.projectservice.common.dto.response.ValidationErrorResponse;
import com.zodiakk.projectservice.task.dto.request.create.TaskTypeCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.filter.TaskTypeFilterDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskTypeUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskTypeResponseDto;
import com.zodiakk.projectservice.task.service.TaskTypeService;
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
@RequestMapping("/api/v1/task-types")
@RequiredArgsConstructor
@Tag(name = "Task types", description = "Task type catalog")
public class TaskTypeController {

    private final TaskTypeService taskTypeService;

    @GetMapping
    @Operation(summary = "Get task types", description = "Returns a paginated list of task types")
    @ApiResponse(responseCode = "200", description = "Task types successfully retrieved")
    public ResponseEntity<Page<TaskTypeResponseDto>> findAll(
            @Valid TaskTypeFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(taskTypeService.findAll(filter, pageRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task type", description = "Returns a task type by identifier")
    @ApiResponse(responseCode = "200", description = "Task type found",
            content = @Content(schema = @Schema(implementation = TaskTypeResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Task type not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TaskTypeResponseDto> getById(
            @Parameter(description = "Task type identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskTypeService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create task type", description = "Creates a task type")
    @ApiResponse(responseCode = "201", description = "Task type successfully created",
            content = @Content(schema = @Schema(implementation = TaskTypeResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Task type code already exists",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TaskTypeResponseDto> create(
            @Valid @RequestBody TaskTypeCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskTypeService.create(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update task type", description = "Updates a task type")
    @ApiResponse(responseCode = "200", description = "Task type successfully updated",
            content = @Content(schema = @Schema(implementation = TaskTypeResponseDto.class)))
    public ResponseEntity<TaskTypeResponseDto> update(
            @Parameter(description = "Task type identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody TaskTypeUpdateRequestDto request
    ) {
        return ResponseEntity.ok(taskTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task type", description = "Deletes a task type that is not used by tasks")
    @ApiResponse(responseCode = "204", description = "Task type successfully deleted")
    @ApiResponse(responseCode = "409", description = "Task type is in use",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Task type identifier", required = true) @PathVariable UUID id
    ) {
        taskTypeService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
