package com.zodiakk.projectservice.task.controller;

import com.zodiakk.projectservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.projectservice.common.dto.response.ValidationErrorResponse;
import com.zodiakk.projectservice.task.dto.request.action.TaskAssigneesUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskBoardPositionUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskCommentCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskParentUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskRelationCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.TaskSkillCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.ProjectTaskUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.ProjectTaskResponseDto;
import com.zodiakk.projectservice.task.dto.response.TaskActivityResponseDto;
import com.zodiakk.projectservice.task.dto.response.TaskAssigneeResponseDto;
import com.zodiakk.projectservice.task.dto.response.TaskCommentResponseDto;
import com.zodiakk.projectservice.task.dto.response.TaskRelationResponseDto;
import com.zodiakk.projectservice.task.dto.response.TaskSkillResponseDto;
import com.zodiakk.projectservice.task.service.ProjectTaskService;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import com.zodiakk.projectservice.task.service.TaskAssigneeService;
import com.zodiakk.projectservice.task.service.TaskCommentService;
import com.zodiakk.projectservice.task.service.TaskRelationService;
import com.zodiakk.projectservice.task.service.TaskSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Operations on project tasks")
public class ProjectTaskController {

    private final ProjectTaskService projectTaskService;

    private final TaskAssigneeService taskAssigneeService;

    private final TaskCommentService taskCommentService;

    private final TaskActivityService taskActivityService;

    private final TaskRelationService taskRelationService;

    private final TaskSkillService taskSkillService;

    @GetMapping("/{id}")
    @Operation(summary = "Get task", description = "Returns task details by identifier")
    @ApiResponse(responseCode = "200", description = "Task found",
            content = @Content(schema = @Schema(implementation = ProjectTaskResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectTaskResponseDto> getById(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(projectTaskService.getById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update task", description = "Updates task fields")
    @ApiResponse(responseCode = "200", description = "Task successfully updated",
            content = @Content(schema = @Schema(implementation = ProjectTaskResponseDto.class)))
    public ResponseEntity<ProjectTaskResponseDto> update(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectTaskUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectTaskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive task", description = "Archives a task that has no child tasks")
    @ApiResponse(responseCode = "204", description = "Task successfully archived")
    @ApiResponse(responseCode = "409", description = "Task has children",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Void> archive(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id
    ) {
        projectTaskService.archive(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child tasks", description = "Returns direct child tasks ordered by board position")
    @ApiResponse(responseCode = "200", description = "Child tasks successfully retrieved")
    public ResponseEntity<List<ProjectTaskResponseDto>> findChildren(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(projectTaskService.findChildren(id));
    }

    @PatchMapping("/{id}/parent")
    @Operation(summary = "Change task parent", description = "Moves the task under another parent or detaches it")
    @ApiResponse(responseCode = "200", description = "Parent successfully changed",
            content = @Content(schema = @Schema(implementation = ProjectTaskResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Hierarchy violation",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectTaskResponseDto> changeParent(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody TaskParentUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectTaskService.changeParent(id, request));
    }

    @PatchMapping("/{id}/board-position")
    @Operation(summary = "Move task card", description = "Moves the task card to another column or position")
    @ApiResponse(responseCode = "200", description = "Task successfully moved",
            content = @Content(schema = @Schema(implementation = ProjectTaskResponseDto.class)))
    public ResponseEntity<ProjectTaskResponseDto> move(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody TaskBoardPositionUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectTaskService.move(id, request));
    }

    @GetMapping("/{id}/assignees")
    @Operation(summary = "Get task assignees", description = "Returns active task assignees")
    @ApiResponse(responseCode = "200", description = "Assignees successfully retrieved")
    public ResponseEntity<List<TaskAssigneeResponseDto>> findAssignees(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskAssigneeService.findAll(id));
    }

    @PutMapping("/{id}/assignees")
    @Operation(summary = "Replace task assignees", description = "Replaces the set of active task assignees")
    @ApiResponse(responseCode = "200", description = "Assignees successfully replaced")
    @ApiResponse(responseCode = "409", description = "Assignment does not belong to the task project",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<List<TaskAssigneeResponseDto>> replaceAssignees(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody TaskAssigneesUpdateRequestDto request
    ) {
        return ResponseEntity.ok(taskAssigneeService.replace(id, request));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "Get task comments", description = "Returns task comments ordered by creation time")
    @ApiResponse(responseCode = "200", description = "Comments successfully retrieved")
    public ResponseEntity<List<TaskCommentResponseDto>> findComments(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskCommentService.findAll(id));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add task comment", description = "Adds a comment to the task")
    @ApiResponse(responseCode = "201", description = "Comment successfully created",
            content = @Content(schema = @Schema(implementation = TaskCommentResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    public ResponseEntity<TaskCommentResponseDto> createComment(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody TaskCommentCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskCommentService.create(id, request));
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Get task activity history", description = "Returns task change history ordered by creation time")
    @ApiResponse(responseCode = "200", description = "Activity history successfully retrieved")
    public ResponseEntity<List<TaskActivityResponseDto>> findActivities(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskActivityService.findAll(id));
    }

    @GetMapping("/{id}/relations")
    @Operation(summary = "Get task relations", description = "Returns relations where the task is the source")
    @ApiResponse(responseCode = "200", description = "Relations successfully retrieved")
    public ResponseEntity<List<TaskRelationResponseDto>> findRelations(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskRelationService.findAll(id));
    }

    @PostMapping("/{id}/relations")
    @Operation(summary = "Create task relation", description = "Creates a relation to another task of the same project")
    @ApiResponse(responseCode = "201", description = "Relation successfully created",
            content = @Content(schema = @Schema(implementation = TaskRelationResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Relation conflicts with project or dependency rules",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TaskRelationResponseDto> createRelation(
            @Parameter(description = "Source task identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody TaskRelationCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskRelationService.create(id, request));
    }

    @GetMapping("/{id}/skills")
    @Operation(summary = "Get task skills", description = "Returns skills required by the task")
    @ApiResponse(responseCode = "200", description = "Task skills successfully retrieved")
    public ResponseEntity<List<TaskSkillResponseDto>> findSkills(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskSkillService.findAll(id));
    }

    @PostMapping("/{id}/skills")
    @Operation(summary = "Add task skill", description = "Adds a required skill to the task")
    @ApiResponse(responseCode = "201", description = "Task skill successfully created",
            content = @Content(schema = @Schema(implementation = TaskSkillResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Skill already added",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TaskSkillResponseDto> createSkill(
            @Parameter(description = "Task identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody TaskSkillCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskSkillService.create(id, request));
    }
}
