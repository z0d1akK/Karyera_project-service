package com.zodiakk.projectservice.project.controller;

import com.zodiakk.projectservice.board.dto.request.action.BoardColumnOrderUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.create.BoardColumnCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.response.BoardColumnResponseDto;
import com.zodiakk.projectservice.board.dto.response.ProjectBoardResponseDto;
import com.zodiakk.projectservice.board.service.BoardService;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.projectservice.common.dto.response.ValidationErrorResponse;
import com.zodiakk.projectservice.project.dto.request.action.ProjectStatusUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectAssignmentCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectLinkCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectMilestoneCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectAssignmentFilterDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectFilterDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectMilestoneFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectAssignmentResponseDto;
import com.zodiakk.projectservice.project.dto.response.ProjectLinkResponseDto;
import com.zodiakk.projectservice.project.dto.response.ProjectMilestoneResponseDto;
import com.zodiakk.projectservice.project.dto.response.ProjectResponseDto;
import com.zodiakk.projectservice.project.service.ProjectAssignmentService;
import com.zodiakk.projectservice.project.service.ProjectLinkService;
import com.zodiakk.projectservice.project.service.ProjectMilestoneService;
import com.zodiakk.projectservice.project.service.ProjectService;
import com.zodiakk.projectservice.task.dto.request.create.ProjectTaskCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.filter.ProjectTaskFilterDto;
import com.zodiakk.projectservice.task.dto.response.ProjectTaskResponseDto;
import com.zodiakk.projectservice.task.service.ProjectTaskService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management, boards, tasks, assignments, milestones and links")
public class ProjectController {

    private final ProjectService projectService;

    private final ProjectAssignmentService projectAssignmentService;

    private final ProjectMilestoneService projectMilestoneService;

    private final ProjectLinkService projectLinkService;

    private final BoardService boardService;

    private final ProjectTaskService projectTaskService;

    @GetMapping
    @Operation(summary = "Search projects", description = "Returns a paginated and filtered list of projects visible to the current user")
    @ApiResponse(responseCode = "200", description = "Projects successfully retrieved")
    public ResponseEntity<Page<ProjectResponseDto>> findAll(
            @Valid ProjectFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(projectService.findAll(filter, pageRequest));
    }

    @PostMapping
    @Operation(summary = "Create project", description = "Creates a new project")
    @ApiResponse(responseCode = "201", description = "Project successfully created",
            content = @Content(schema = @Schema(implementation = ProjectResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Project code already exists",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectResponseDto> create(@Valid @RequestBody ProjectCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project", description = "Returns project details by identifier")
    @ApiResponse(responseCode = "200", description = "Project found",
            content = @Content(schema = @Schema(implementation = ProjectResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectResponseDto> getById(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update project", description = "Updates project fields")
    @ApiResponse(responseCode = "200", description = "Project successfully updated",
            content = @Content(schema = @Schema(implementation = ProjectResponseDto.class)))
    public ResponseEntity<ProjectResponseDto> update(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change project status", description = "Changes project status according to the allowed transition matrix")
    @ApiResponse(responseCode = "200", description = "Project status successfully changed",
            content = @Content(schema = @Schema(implementation = ProjectResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Invalid status transition",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectResponseDto> changeStatus(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectStatusUpdateRequestDto request
    ) {
        return ResponseEntity.ok(projectService.changeStatus(id, request));
    }

    @GetMapping("/{id}/board")
    @Operation(summary = "Get project board", description = "Returns the project board with columns and task cards")
    @ApiResponse(responseCode = "200", description = "Board successfully retrieved",
            content = @Content(schema = @Schema(implementation = ProjectBoardResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Board not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectBoardResponseDto> getBoard(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(boardService.getByProject(id));
    }

    @PostMapping("/{id}/board")
    @Operation(summary = "Create project board", description = "Creates the board for a project")
    @ApiResponse(responseCode = "201", description = "Board successfully created",
            content = @Content(schema = @Schema(implementation = ProjectBoardResponseDto.class)))
    public ResponseEntity<ProjectBoardResponseDto> createBoard(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @RequestBody(required = false) ProjectBoardUpdateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.create(id, request));
    }

    @PostMapping("/{id}/board/columns")
    @Operation(summary = "Add board column", description = "Adds a column to the project board")
    @ApiResponse(responseCode = "201", description = "Column successfully created",
            content = @Content(schema = @Schema(implementation = BoardColumnResponseDto.class)))
    public ResponseEntity<BoardColumnResponseDto> addColumn(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody BoardColumnCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.addColumn(id, request));
    }

    @PatchMapping("/{id}/board/columns/order")
    @Operation(summary = "Reorder board columns", description = "Replaces the order of the board columns")
    @ApiResponse(responseCode = "200", description = "Columns successfully reordered",
            content = @Content(schema = @Schema(implementation = ProjectBoardResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Column set does not match the board",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectBoardResponseDto> reorderColumns(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody BoardColumnOrderUpdateRequestDto request
    ) {
        return ResponseEntity.ok(boardService.reorderColumns(id, request));
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "Search project tasks", description = "Returns a paginated and filtered list of project tasks")
    @ApiResponse(responseCode = "200", description = "Tasks successfully retrieved")
    public ResponseEntity<Page<ProjectTaskResponseDto>> findTasks(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid ProjectTaskFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(projectTaskService.findAll(id, filter, pageRequest));
    }

    @PostMapping("/{id}/tasks")
    @Operation(summary = "Create task", description = "Creates a task in the project")
    @ApiResponse(responseCode = "201", description = "Task successfully created",
            content = @Content(schema = @Schema(implementation = ProjectTaskResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Task hierarchy violation",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectTaskResponseDto> createTask(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectTaskCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectTaskService.create(id, request));
    }

    @GetMapping("/{id}/assignments")
    @Operation(summary = "Get project assignments", description = "Returns a paginated list of project participants")
    @ApiResponse(responseCode = "200", description = "Assignments successfully retrieved")
    public ResponseEntity<Page<ProjectAssignmentResponseDto>> findAssignments(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid ProjectAssignmentFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        filter.setProjectId(id);

        return ResponseEntity.ok(projectAssignmentService.findAll(filter, pageRequest));
    }

    @PostMapping("/{id}/assignments")
    @Operation(summary = "Add project participant", description = "Creates a project assignment for an employee")
    @ApiResponse(responseCode = "201", description = "Assignment successfully created",
            content = @Content(schema = @Schema(implementation = ProjectAssignmentResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Assignment already exists",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProjectAssignmentResponseDto> createAssignment(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectAssignmentCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectAssignmentService.create(id, request));
    }

    @GetMapping("/{id}/milestones")
    @Operation(summary = "Get project milestones", description = "Returns a paginated list of project milestones")
    @ApiResponse(responseCode = "200", description = "Milestones successfully retrieved")
    public ResponseEntity<Page<ProjectMilestoneResponseDto>> findMilestones(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid ProjectMilestoneFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        filter.setProjectId(id);

        return ResponseEntity.ok(projectMilestoneService.findAll(filter, pageRequest));
    }

    @PostMapping("/{id}/milestones")
    @Operation(summary = "Create project milestone", description = "Creates a milestone for the project")
    @ApiResponse(responseCode = "201", description = "Milestone successfully created",
            content = @Content(schema = @Schema(implementation = ProjectMilestoneResponseDto.class)))
    public ResponseEntity<ProjectMilestoneResponseDto> createMilestone(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectMilestoneCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectMilestoneService.create(id, request));
    }

    @GetMapping("/{id}/links")
    @Operation(summary = "Get project links", description = "Returns external links of the project")
    @ApiResponse(responseCode = "200", description = "Links successfully retrieved")
    public ResponseEntity<List<ProjectLinkResponseDto>> findLinks(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(projectLinkService.findAll(id));
    }

    @PostMapping("/{id}/links")
    @Operation(summary = "Create project link", description = "Adds an external link to the project")
    @ApiResponse(responseCode = "201", description = "Link successfully created",
            content = @Content(schema = @Schema(implementation = ProjectLinkResponseDto.class)))
    public ResponseEntity<ProjectLinkResponseDto> createLink(
            @Parameter(description = "Project identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectLinkCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectLinkService.create(id, request));
    }
}
