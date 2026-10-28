package com.zodiakk.projectservice.board.service.impl;

import com.zodiakk.projectservice.board.dto.request.action.BoardColumnOrderUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.create.BoardColumnCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.BoardColumnUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.response.BoardColumnResponseDto;
import com.zodiakk.projectservice.board.dto.response.ProjectBoardResponseDto;
import com.zodiakk.projectservice.board.dto.response.TaskCardResponseDto;
import com.zodiakk.projectservice.board.entity.BoardColumn;
import com.zodiakk.projectservice.board.entity.ProjectBoard;
import com.zodiakk.projectservice.board.exception.board.ProjectBoardAlreadyExistsException;
import com.zodiakk.projectservice.board.exception.board.ProjectBoardNotFoundException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnCodeAlreadyExistsException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnInUseException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnNotFoundException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnOrderMismatchException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnPositionAlreadyExistsException;
import com.zodiakk.projectservice.board.mapper.BoardColumnMapper;
import com.zodiakk.projectservice.board.mapper.ProjectBoardMapper;
import com.zodiakk.projectservice.board.repository.BoardColumnRepository;
import com.zodiakk.projectservice.board.repository.ProjectBoardRepository;
import com.zodiakk.projectservice.board.service.BoardService;
import com.zodiakk.projectservice.common.enums.TaskRelationType;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskAssignee;
import com.zodiakk.projectservice.task.entity.TaskRelation;
import com.zodiakk.projectservice.task.entity.TaskType;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.repository.TaskRelationRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardServiceImpl implements BoardService {

    private static final int POSITION_OFFSET = 1_000;

    private final ProjectBoardRepository projectBoardRepository;

    private final BoardColumnRepository boardColumnRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final TaskAssigneeRepository taskAssigneeRepository;

    private final TaskRelationRepository taskRelationRepository;

    private final TaskTypeRepository taskTypeRepository;

    private final ProjectRepository projectRepository;

    private final ProjectBoardMapper projectBoardMapper;

    private final BoardColumnMapper boardColumnMapper;

    @Override
    @PreAuthorize("@projectAccess.canViewProject(#projectId)")
    public ProjectBoardResponseDto getByProject(UUID projectId) {
        ProjectBoard board = projectBoardRepository
                .findFirstByProjectIdOrderByCreatedAtAsc(projectId)
                .orElseThrow(() -> new ProjectBoardNotFoundException(projectId));

        List<BoardColumn> columns = boardColumnRepository.findAllByBoardIdOrderByPositionAsc(board.getId());

        List<ProjectTask> tasks = projectTaskRepository
                .findAllByProjectIdAndArchivedAtIsNullOrderByBoardPositionAsc(projectId);

        Map<UUID, List<ProjectTask>> tasksByColumn = tasks.stream()
                .collect(Collectors.groupingBy(ProjectTask::getColumnId));

        Map<UUID, String> typeCodes = resolveTypeCodes(tasks);
        Map<UUID, List<UUID>> assignees = resolveAssignees(tasks);
        Set<UUID> blocked = resolveBlockedTasks(tasks);
        String projectCode = projectRepository.findById(projectId)
                .map(project -> project.getCode())
                .orElse("");

        List<BoardColumnResponseDto> columnDtos = columns.stream()
                .map(column -> toColumnResponse(column, tasksByColumn, typeCodes, assignees, blocked, projectCode))
                .toList();

        ProjectBoardResponseDto response = projectBoardMapper.toResponseDto(board);
        response.setColumns(columnDtos);

        return response;
    }

    @Override
    @PreAuthorize("@projectAccess.canEditProject(#projectId)")
    @Transactional
    public ProjectBoardResponseDto create(UUID projectId, ProjectBoardUpdateRequestDto request) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        if (projectBoardRepository.existsByProjectId(projectId)) {
            throw new ProjectBoardAlreadyExistsException(projectId);
        }

        ProjectBoard board = ProjectBoard.builder()
                .projectId(projectId)
                .name(resolveBoardName(request))
                .build();

        ProjectBoardResponseDto response = projectBoardMapper.toResponseDto(projectBoardRepository.save(board));
        response.setColumns(List.of());

        return response;
    }

    @Override
    @PreAuthorize("@projectAccess.canEditBoard(#id)")
    @Transactional
    public ProjectBoardResponseDto update(UUID id, ProjectBoardUpdateRequestDto request) {
        ProjectBoard board = findBoard(id);

        projectBoardMapper.updateEntity(request, board);

        ProjectBoardResponseDto response = projectBoardMapper.toResponseDto(projectBoardRepository.save(board));
        response.setColumns(
                boardColumnRepository.findAllByBoardIdOrderByPositionAsc(board.getId())
                        .stream()
                        .map(boardColumnMapper::toResponseDto)
                        .toList()
        );

        return response;
    }

    @Override
    @PreAuthorize("@projectAccess.canEditProject(#projectId)")
    @Transactional
    public BoardColumnResponseDto addColumn(UUID projectId, BoardColumnCreateRequestDto request) {
        ProjectBoard board = projectBoardRepository
                .findFirstByProjectIdOrderByCreatedAtAsc(projectId)
                .orElseThrow(() -> new ProjectBoardNotFoundException(projectId));

        request.setBoardId(board.getId());

        if (request.getPosition() == null || request.getPosition() < 0) {
            throw new BoardColumnPositionAlreadyExistsException(request.getPosition());
        }

        if (boardColumnRepository.existsByBoardIdAndCodeIgnoreCase(board.getId(), request.getCode())) {
            throw new BoardColumnCodeAlreadyExistsException(request.getCode());
        }

        if (boardColumnRepository.existsByBoardIdAndPosition(board.getId(), request.getPosition())) {
            throw new BoardColumnPositionAlreadyExistsException(request.getPosition());
        }

        if (Boolean.TRUE.equals(request.getIsInitial())) {
            clearInitialColumn(board.getId());
        }

        BoardColumn column = boardColumnMapper.toEntity(request);

        return toColumnResponse(boardColumnRepository.save(column));
    }

    @Override
    @PreAuthorize("@projectAccess.canEditBoardColumn(#id)")
    @Transactional
    public BoardColumnResponseDto updateColumn(UUID id, BoardColumnUpdateRequestDto request) {
        BoardColumn column = findColumn(id);

        if (request.getCode() != null
                && !request.getCode().isBlank()
                && !request.getCode().equalsIgnoreCase(column.getCode())
                && boardColumnRepository.existsByBoardIdAndCodeIgnoreCaseAndIdNot(
                column.getBoardId(),
                request.getCode(),
                column.getId()
        )) {
            throw new BoardColumnCodeAlreadyExistsException(request.getCode());
        }

        if (request.getPosition() != null) {
            if (request.getPosition() < 0) {
                throw new BoardColumnPositionAlreadyExistsException(request.getPosition());
            }

            if (!request.getPosition().equals(column.getPosition())
                    && boardColumnRepository.existsByBoardIdAndPosition(column.getBoardId(), request.getPosition())) {
                throw new BoardColumnPositionAlreadyExistsException(request.getPosition());
            }
        }

        if (Boolean.TRUE.equals(request.getIsInitial())) {
            clearInitialColumn(column.getBoardId(), column.getId());
        }

        boardColumnMapper.updateEntity(request, column);

        return toColumnResponse(boardColumnRepository.save(column));
    }

    @Override
    @PreAuthorize("@projectAccess.canEditBoardColumn(#id)")
    @Transactional
    public void deleteColumn(UUID id) {
        BoardColumn column = findColumn(id);

        long taskCount = projectTaskRepository.countByColumnIdAndArchivedAtIsNull(column.getId());

        if (taskCount > 0) {
            throw new BoardColumnInUseException(column.getId());
        }

        boardColumnRepository.delete(column);
    }

    @Override
    @PreAuthorize("@projectAccess.canEditProject(#projectId)")
    @Transactional
    public ProjectBoardResponseDto reorderColumns(UUID projectId, BoardColumnOrderUpdateRequestDto request) {
        ProjectBoard board = projectBoardRepository
                .findFirstByProjectIdOrderByCreatedAtAsc(projectId)
                .orElseThrow(() -> new ProjectBoardNotFoundException(projectId));

        List<BoardColumn> columns = boardColumnRepository.findAllByBoardIdOrderByPositionAsc(board.getId());

        Set<UUID> requestedIds = new LinkedHashSet<>(request.getColumnIds());
        Set<UUID> existingIds = columns.stream()
                .map(BoardColumn::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!requestedIds.equals(existingIds) || requestedIds.contains(null)) {
            throw new BoardColumnOrderMismatchException();
        }

        for (BoardColumn column : columns) {
            column.setPosition(column.getPosition() + POSITION_OFFSET);
        }
        boardColumnRepository.saveAllAndFlush(columns);

        for (int index = 0; index < request.getColumnIds().size(); index++) {
            UUID columnId = request.getColumnIds().get(index);
            BoardColumn column = columns.stream()
                    .filter(candidate -> candidate.getId().equals(columnId))
                    .findFirst()
                    .orElseThrow(() -> new BoardColumnNotFoundException(columnId));

            column.setPosition(index);
        }
        boardColumnRepository.saveAllAndFlush(columns);

        ProjectBoardResponseDto response = projectBoardMapper.toResponseDto(board);
        response.setColumns(
                boardColumnRepository.findAllByBoardIdOrderByPositionAsc(board.getId())
                        .stream()
                        .map(this::toColumnResponse)
                        .toList()
        );

        return response;
    }

    private ProjectBoard findBoard(UUID id) {
        return projectBoardRepository.findById(id)
                .orElseThrow(() -> new ProjectBoardNotFoundException(id));
    }

    private BoardColumn findColumn(UUID id) {
        return boardColumnRepository.findById(id)
                .orElseThrow(() -> new BoardColumnNotFoundException(id));
    }

    private String resolveBoardName(ProjectBoardUpdateRequestDto request) {
        if (request != null && request.getName() != null && !request.getName().isBlank()) {
            return request.getName();
        }

        return "Main board";
    }

    private void clearInitialColumn(UUID boardId) {
        clearInitialColumn(boardId, null);
    }

    private void clearInitialColumn(UUID boardId, UUID exceptId) {
        boardColumnRepository.findFirstByBoardIdAndIsInitialTrue(boardId)
                .filter(column -> !column.getId().equals(exceptId))
                .ifPresent(column -> {
                    column.setIsInitial(false);
                    boardColumnRepository.save(column);
                });
    }

    private BoardColumnResponseDto toColumnResponse(BoardColumn column) {
        BoardColumnResponseDto response = boardColumnMapper.toResponseDto(column);
        response.setTaskCount(projectTaskRepository.countByColumnIdAndArchivedAtIsNull(column.getId()));
        response.setTasks(List.of());

        return response;
    }

    private BoardColumnResponseDto toColumnResponse(
            BoardColumn column,
            Map<UUID, List<ProjectTask>> tasksByColumn,
            Map<UUID, String> typeCodes,
            Map<UUID, List<UUID>> assignees,
            Set<UUID> blocked,
            String projectCode
    ) {
        BoardColumnResponseDto response = boardColumnMapper.toResponseDto(column);

        List<ProjectTask> columnTasks = tasksByColumn.getOrDefault(column.getId(), List.of());

        response.setTaskCount((long) columnTasks.size());
        response.setTasks(
                columnTasks.stream()
                        .map(task -> toCard(task, typeCodes, assignees, blocked, projectCode))
                        .toList()
        );

        return response;
    }

    private TaskCardResponseDto toCard(
            ProjectTask task,
            Map<UUID, String> typeCodes,
            Map<UUID, List<UUID>> assignees,
            Set<UUID> blocked,
            String projectCode
    ) {
        return TaskCardResponseDto.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .taskNumber(task.getTaskNumber())
                .key(projectCode + "-" + task.getTaskNumber())
                .title(task.getTitle())
                .type(typeCodes.get(task.getTaskTypeId()))
                .priority(task.getPriority())
                .columnId(task.getColumnId())
                .boardPosition(task.getBoardPosition())
                .blocked(blocked.contains(task.getId()))
                .assigneeIds(assignees.getOrDefault(task.getId(), List.of()))
                .build();
    }

    private Map<UUID, String> resolveTypeCodes(List<ProjectTask> tasks) {
        Set<UUID> typeIds = tasks.stream()
                .map(ProjectTask::getTaskTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (typeIds.isEmpty()) {
            return Map.of();
        }

        return taskTypeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(TaskType::getId, TaskType::getCode));
    }

    private Map<UUID, List<UUID>> resolveAssignees(List<ProjectTask> tasks) {
        Set<UUID> taskIds = tasks.stream()
                .map(ProjectTask::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (taskIds.isEmpty()) {
            return Map.of();
        }

        return taskAssigneeRepository.findAllByTaskIdInAndUnassignedAtIsNull(taskIds).stream()
                .collect(Collectors.groupingBy(
                        TaskAssignee::getTaskId,
                        Collectors.mapping(TaskAssignee::getAssignmentId, Collectors.toList())
                ));
    }

    private Set<UUID> resolveBlockedTasks(List<ProjectTask> tasks) {
        if (tasks.isEmpty()) {
            return Set.of();
        }

        Map<UUID, ProjectTask> tasksById = tasks.stream()
                .collect(Collectors.toMap(ProjectTask::getId, Function.identity(), (first, second) -> first));

        List<TaskRelation> blockingRelations = taskRelationRepository
                .findAllByTargetTaskIdInAndRelationType(tasksById.keySet(), TaskRelationType.BLOCKS);

        Set<UUID> blocked = new HashSet<>();

        for (TaskRelation relation : blockingRelations) {
            ProjectTask source = tasksById.get(relation.getSourceTaskId());

            if (source != null && source.getCompletedAt() == null) {
                blocked.add(relation.getTargetTaskId());
            }
        }

        return blocked;
    }
}
