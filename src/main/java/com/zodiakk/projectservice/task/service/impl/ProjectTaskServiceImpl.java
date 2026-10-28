package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.board.entity.BoardColumn;
import com.zodiakk.projectservice.board.entity.ProjectBoard;
import com.zodiakk.projectservice.board.exception.column.BoardColumnNotFoundException;
import com.zodiakk.projectservice.board.repository.BoardColumnRepository;
import com.zodiakk.projectservice.board.repository.ProjectBoardRepository;
import com.zodiakk.projectservice.common.constants.EventConstants;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.enums.TaskRelationType;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.outbox.service.OutboxEventService;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.task.dto.request.action.TaskBoardPositionUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskParentUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.ProjectTaskCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.filter.ProjectTaskFilterDto;
import com.zodiakk.projectservice.task.dto.request.update.ProjectTaskUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.ProjectTaskResponseDto;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskRelation;
import com.zodiakk.projectservice.task.entity.TaskType;
import com.zodiakk.projectservice.task.exception.task.CrossProjectParentException;
import com.zodiakk.projectservice.task.exception.task.InvalidParentTaskTypeException;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.exception.task.TaskArchivedException;
import com.zodiakk.projectservice.task.exception.task.TaskHasChildrenException;
import com.zodiakk.projectservice.task.exception.task.TaskHierarchyCycleException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeInactiveException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeNotFoundException;
import com.zodiakk.projectservice.task.mapper.ProjectTaskMapper;
import com.zodiakk.projectservice.task.mapper.TaskAssigneeMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.repository.TaskRelationRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
import com.zodiakk.projectservice.task.repository.specification.projectTask.ProjectTaskPageableFactory;
import com.zodiakk.projectservice.task.repository.specification.projectTask.ProjectTaskSpecification;
import com.zodiakk.projectservice.task.service.ProjectTaskService;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectTaskServiceImpl implements ProjectTaskService {

    private static final String ACTION_CREATED = "TASK_CREATED";

    private static final String ACTION_UPDATED = "TASK_UPDATED";

    private static final String ACTION_MOVED = "TASK_MOVED";

    private static final String ACTION_ARCHIVED = "TASK_ARCHIVED";

    private static final String ACTION_PARENT_CHANGED = "TASK_PARENT_CHANGED";

    private static final BigDecimal POSITION_STEP = BigDecimal.valueOf(1024);

    private static final BigDecimal MIN_POSITION_GAP = BigDecimal.valueOf(0.000001);

    private static final Map<String, Set<String>> ALLOWED_CHILDREN = Map.of(
            "EPIC", Set.of("STORY", "TASK", "BUG"),
            "STORY", Set.of("SUBTASK"),
            "TASK", Set.of("SUBTASK"),
            "BUG", Set.of("SUBTASK")
    );

    private final ProjectTaskRepository projectTaskRepository;

    private final ProjectRepository projectRepository;

    private final BoardColumnRepository boardColumnRepository;

    private final ProjectBoardRepository projectBoardRepository;

    private final TaskTypeRepository taskTypeRepository;

    private final TaskAssigneeRepository taskAssigneeRepository;

    private final TaskRelationRepository taskRelationRepository;

    private final ProjectTaskMapper projectTaskMapper;

    private final TaskAssigneeMapper taskAssigneeMapper;

    private final TaskActivityService taskActivityService;

    private final OutboxEventService outboxEventService;

    private final EmployeeDirectoryService employeeDirectoryService;

    @Override
    @PreAuthorize("@projectAccess.canViewProject(#projectId)")
    public Page<ProjectTaskResponseDto> findAll(
            UUID projectId,
            ProjectTaskFilterDto filter,
            PageRequestDto pageRequest
    ) {
        filter.setProjectId(projectId);

        if (filter.getArchived() == null) {
            filter.setArchived(false);
        }

        Pageable pageable = ProjectTaskPageableFactory.create(pageRequest);

        Page<ProjectTask> page = projectTaskRepository.findAll(
                ProjectTaskSpecification.filter(filter),
                pageable
        );

        String projectCode = projectRepository.findById(projectId)
                .map(Project::getCode)
                .orElse("");

        return page.map(task -> toResponseDto(task, projectCode));
    }

    @Override
    @PreAuthorize("@projectAccess.canViewTask(#id)")
    public ProjectTaskResponseDto getById(UUID id) {
        ProjectTask task = findById(id);
        Project project = requireProject(task.getProjectId());

        return toResponseDto(task, project.getCode());
    }

    @Override
    @PreAuthorize("@projectAccess.canEditProject(#projectId)")
    @Transactional
    public ProjectTaskResponseDto create(UUID projectId, ProjectTaskCreateRequestDto request) {
        request.setProjectId(projectId);

        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        TaskType taskType = requireActiveTaskType(request.getTaskTypeId());
        BoardColumn column = requireColumn(projectId, request.getColumnId());

        if (request.getParentTaskId() != null) {
            validateParent(projectId, taskType, request.getParentTaskId(), null);
        }

        UUID reporterEmployeeId = employeeDirectoryService.getCurrentEmployee().getEmployeeId();

        Long nextTaskNumber = projectTaskRepository.findMaxTaskNumber(projectId) + 1;
        BigDecimal position = nextPosition(column.getId());

        ProjectTask task = projectTaskMapper.toEntity(request);
        task.setTaskNumber(nextTaskNumber);
        task.setBoardPosition(position);
        task.setReporterEmployeeId(reporterEmployeeId);
        task.setCompletedAt(Boolean.TRUE.equals(column.getIsTerminal()) ? now() : null);

        ProjectTask saved = projectTaskRepository.save(task);

        taskActivityService.record(
                saved.getId(),
                reporterEmployeeId,
                ACTION_CREATED,
                payload(
                        "title", saved.getTitle(),
                        "taskTypeId", saved.getTaskTypeId(),
                        "columnId", saved.getColumnId(),
                        "parentTaskId", saved.getParentTaskId()
                )
        );

        if (saved.getCompletedAt() != null) {
            saveCompletedEvent(saved, null);
        }

        return toResponseDto(saved, project.getCode());
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTask(#id)")
    @Transactional
    public ProjectTaskResponseDto update(UUID id, ProjectTaskUpdateRequestDto request) {
        ProjectTask task = projectTaskRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProjectTaskNotFoundException(id));

        if (task.getArchivedAt() != null) {
            throw new TaskArchivedException(id);
        }

        Project project = requireProject(task.getProjectId());

        Map<String, Object> changes = new LinkedHashMap<>();

        if (request.getTaskTypeId() != null && !request.getTaskTypeId().equals(task.getTaskTypeId())) {
            TaskType taskType = requireActiveTaskType(request.getTaskTypeId());
            validateChildrenTypes(task, taskType);

            if (task.getParentTaskId() != null) {
                validateParent(task.getProjectId(), taskType, task.getParentTaskId(), task.getId());
            }

            changes.put("taskTypeId", task.getTaskTypeId());
        }

        UUID previousColumnId = task.getColumnId();

        if (request.getColumnId() != null && !request.getColumnId().equals(task.getColumnId())) {
            BoardColumn column = requireColumn(task.getProjectId(), request.getColumnId());
            applyColumnChange(task, column);
            changes.put("columnId", previousColumnId);
        }

        if (request.getParentTaskId() != null
                && !request.getParentTaskId().equals(task.getParentTaskId())) {
            TaskType taskType = requireTaskType(task.getTaskTypeId());
            validateParent(task.getProjectId(), taskType, request.getParentTaskId(), task.getId());
            changes.put("parentTaskId", task.getParentTaskId());
        }

        if (request.getComplexity() != null && request.getComplexity() != task.getComplexity()) {
            changes.put("complexity", task.getComplexity());
        }

        addChange(changes, "title", task.getTitle(), request.getTitle());
        addChange(changes, "description", task.getDescription(), request.getDescription());
        addChange(changes, "priority", task.getPriority(), request.getPriority());
        addChange(changes, "storyPoints", task.getStoryPoints(), request.getStoryPoints());
        addChange(changes, "estimatedMinutes", task.getEstimatedMinutes(), request.getEstimatedMinutes());
        addChange(changes, "dueDate", task.getDueDate(), request.getDueDate());

        var previousComplexity = task.getComplexity();

        projectTaskMapper.updateEntity(request, task);

        ProjectTask saved = projectTaskRepository.save(task);

        if (!changes.isEmpty()) {
            UUID actorEmployeeId = currentEmployeeId();
            taskActivityService.record(saved.getId(), actorEmployeeId, ACTION_UPDATED, changes);
        }

        if (previousColumnId != null && !previousColumnId.equals(saved.getColumnId())) {
            emitCompletionEvents(saved, previousColumnId);
        }

        if (request.getComplexity() != null
                && request.getComplexity() != previousComplexity) {
            outboxEventService.saveEvent(
                    saved.getId(),
                    EventConstants.AGGREGATE_PROJECT_TASK,
                    EventConstants.PROJECT_TASK_COMPLEXITY_CHANGED,
                    EventConstants.DEFAULT_EVENT_VERSION,
                    payload(
                            "taskId", saved.getId(),
                            "projectId", saved.getProjectId(),
                            "previousComplexity", previousComplexity,
                            "complexity", saved.getComplexity()
                    )
            );
        }

        return toResponseDto(saved, project.getCode());
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTask(#id)")
    @Transactional
    public void archive(UUID id) {
        ProjectTask task = projectTaskRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProjectTaskNotFoundException(id));

        if (task.getArchivedAt() != null) {
            throw new TaskArchivedException(id);
        }

        if (projectTaskRepository.countByParentTaskId(id) > 0) {
            throw new TaskHasChildrenException(id);
        }

        task.setArchivedAt(now());
        ProjectTask saved = projectTaskRepository.save(task);

        taskActivityService.record(
                saved.getId(),
                currentEmployeeId(),
                ACTION_ARCHIVED,
                payload("archivedAt", saved.getArchivedAt())
        );
    }

    @Override
    @PreAuthorize("@projectAccess.canViewTask(#id)")
    public List<ProjectTaskResponseDto> findChildren(UUID id) {
        ProjectTask parent = findById(id);
        String projectCode = requireProject(parent.getProjectId()).getCode();

        return projectTaskRepository.findAllByParentTaskId(id)
                .stream()
                .sorted(Comparator.comparing(ProjectTask::getBoardPosition))
                .map(child -> toResponseDto(child, projectCode))
                .toList();
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTask(#id)")
    @Transactional
    public ProjectTaskResponseDto changeParent(UUID id, TaskParentUpdateRequestDto request) {
        ProjectTask task = projectTaskRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProjectTaskNotFoundException(id));

        if (task.getArchivedAt() != null) {
            throw new TaskArchivedException(id);
        }

        UUID previousParentId = task.getParentTaskId();

        if (Objects.equals(previousParentId, request.getParentTaskId())) {
            return toResponseDto(task, requireProject(task.getProjectId()).getCode());
        }

        if (request.getParentTaskId() != null) {
            validateParent(
                    task.getProjectId(),
                    requireTaskType(task.getTaskTypeId()),
                    request.getParentTaskId(),
                    task.getId()
            );
        }

        task.setParentTaskId(request.getParentTaskId());
        ProjectTask saved = projectTaskRepository.save(task);

        taskActivityService.record(
                saved.getId(),
                currentEmployeeId(),
                ACTION_PARENT_CHANGED,
                payload("previousParentTaskId", previousParentId, "parentTaskId", saved.getParentTaskId())
        );

        return toResponseDto(saved, requireProject(saved.getProjectId()).getCode());
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTask(#id)")
    @Transactional
    public ProjectTaskResponseDto move(UUID id, TaskBoardPositionUpdateRequestDto request) {
        ProjectTask task = projectTaskRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProjectTaskNotFoundException(id));

        if (task.getArchivedAt() != null) {
            throw new TaskArchivedException(id);
        }

        BoardColumn targetColumn = requireColumn(task.getProjectId(), request.getTargetColumnId());

        ProjectTask beforeTask = requireNeighbour(request.getBeforeTaskId(), targetColumn.getId(), id);
        ProjectTask afterTask = requireNeighbour(request.getAfterTaskId(), targetColumn.getId(), id);

        if (beforeTask != null && afterTask != null
                && beforeTask.getBoardPosition().compareTo(afterTask.getBoardPosition()) >= 0) {
            renumberColumn(targetColumn.getId());
            beforeTask = requireNeighbour(request.getBeforeTaskId(), targetColumn.getId(), id);
            afterTask = requireNeighbour(request.getAfterTaskId(), targetColumn.getId(), id);
        }

        BigDecimal newPosition = calculatePosition(targetColumn.getId(), beforeTask, afterTask);

        UUID previousColumnId = task.getColumnId();

        task.setColumnId(targetColumn.getId());
        task.setBoardPosition(newPosition);

        if (!previousColumnId.equals(targetColumn.getId())) {
            applyCompletionState(task, targetColumn);
        }

        ProjectTask saved = projectTaskRepository.save(task);

        taskActivityService.record(
                saved.getId(),
                currentEmployeeId(),
                ACTION_MOVED,
                payload(
                        "previousColumnId", previousColumnId,
                        "columnId", saved.getColumnId(),
                        "boardPosition", saved.getBoardPosition()
                )
        );

        emitCompletionEvents(saved, previousColumnId);

        return toResponseDto(saved, requireProject(saved.getProjectId()).getCode());
    }

    private ProjectTask findById(UUID id) {
        return projectTaskRepository.findById(id)
                .orElseThrow(() -> new ProjectTaskNotFoundException(id));
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private TaskType requireTaskType(UUID id) {
        return taskTypeRepository.findById(id)
                .orElseThrow(() -> new TaskTypeNotFoundException(id));
    }

    private TaskType requireActiveTaskType(UUID id) {
        TaskType taskType = requireTaskType(id);

        if (!Boolean.TRUE.equals(taskType.getActive())) {
            throw new TaskTypeInactiveException(id);
        }

        return taskType;
    }

    private BoardColumn requireColumn(UUID projectId, UUID columnId) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new BoardColumnNotFoundException(columnId));

        ProjectBoard board = projectBoardRepository.findById(column.getBoardId())
                .orElseThrow(() -> new BoardColumnNotFoundException(columnId));

        if (!projectId.equals(board.getProjectId())) {
            throw new BoardColumnNotFoundException(columnId);
        }

        return column;
    }

    private ProjectTask requireNeighbour(UUID taskId, UUID columnId, UUID movedTaskId) {
        if (taskId == null || taskId.equals(movedTaskId)) {
            return null;
        }

        ProjectTask neighbour = findById(taskId);

        if (!columnId.equals(neighbour.getColumnId())) {
            throw new BoardColumnNotFoundException(columnId);
        }

        return neighbour;
    }

    private void validateParent(UUID projectId, TaskType childType, UUID parentTaskId, UUID taskId) {
        if (parentTaskId.equals(taskId)) {
            throw new TaskHierarchyCycleException();
        }

        ProjectTask parent = findById(parentTaskId);

        if (!projectId.equals(parent.getProjectId())) {
            throw new CrossProjectParentException();
        }

        String parentTypeCode = normalizeTypeCode(requireTaskType(parent.getTaskTypeId()).getCode());

        if (!ALLOWED_CHILDREN.getOrDefault(parentTypeCode, Set.of()).contains(normalizeTypeCode(childType.getCode()))) {
            throw new InvalidParentTaskTypeException(parentTypeCode, childType.getCode());
        }

        if (taskId != null) {
            UUID ancestorId = parent.getParentTaskId();
            Set<UUID> visited = new LinkedHashSet<>();

            while (ancestorId != null) {
                if (ancestorId.equals(taskId)) {
                    throw new TaskHierarchyCycleException();
                }

                if (!visited.add(ancestorId)) {
                    throw new TaskHierarchyCycleException();
                }

                ancestorId = projectTaskRepository.findById(ancestorId)
                        .map(ProjectTask::getParentTaskId)
                        .orElse(null);
            }
        }
    }

    private void validateChildrenTypes(ProjectTask task, TaskType newType) {
        List<ProjectTask> children = projectTaskRepository.findAllByParentTaskId(task.getId());

        if (children.isEmpty()) {
            return;
        }

        Set<String> allowedChildren = ALLOWED_CHILDREN.getOrDefault(normalizeTypeCode(newType.getCode()), Set.of());

        Set<UUID> childTypeIds = children.stream()
                .map(ProjectTask::getTaskTypeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, String> childTypeCodes = taskTypeRepository.findAllById(childTypeIds).stream()
                .collect(Collectors.toMap(TaskType::getId, TaskType::getCode));

        for (ProjectTask child : children) {
            String childTypeCode = childTypeCodes.get(child.getTaskTypeId());

            if (childTypeCode == null || !allowedChildren.contains(normalizeTypeCode(childTypeCode))) {
                throw new InvalidParentTaskTypeException(newType.getCode(), childTypeCode);
            }
        }
    }

    private void applyColumnChange(ProjectTask task, BoardColumn column) {
        task.setColumnId(column.getId());
        task.setBoardPosition(nextPosition(column.getId()));
        applyCompletionState(task, column);
    }

    private void applyCompletionState(ProjectTask task, BoardColumn column) {
        if (Boolean.TRUE.equals(column.getIsTerminal())) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(now());
            }
        } else {
            task.setCompletedAt(null);
        }
    }

    private void emitCompletionEvents(ProjectTask task, UUID previousColumnId) {
        boolean wasCompleted = previousColumnId != null
                && isTerminalColumn(previousColumnId);
        boolean isCompleted = task.getCompletedAt() != null;

        if (!wasCompleted && isCompleted) {
            saveCompletedEvent(task, previousColumnId);
        } else if (wasCompleted && !isCompleted) {
            outboxEventService.saveEvent(
                    task.getId(),
                    EventConstants.AGGREGATE_PROJECT_TASK,
                    EventConstants.PROJECT_TASK_REOPENED,
                    EventConstants.DEFAULT_EVENT_VERSION,
                    payload(
                            "taskId", task.getId(),
                            "projectId", task.getProjectId(),
                            "previousColumnId", previousColumnId,
                            "columnId", task.getColumnId()
                    )
            );
        }
    }

    private void saveCompletedEvent(ProjectTask task, UUID previousColumnId) {
        outboxEventService.saveEvent(
                task.getId(),
                EventConstants.AGGREGATE_PROJECT_TASK,
                EventConstants.PROJECT_TASK_COMPLETED,
                EventConstants.DEFAULT_EVENT_VERSION,
                payload(
                        "taskId", task.getId(),
                        "projectId", task.getProjectId(),
                        "taskNumber", task.getTaskNumber(),
                        "previousColumnId", previousColumnId,
                        "columnId", task.getColumnId(),
                        "completedAt", task.getCompletedAt()
                )
        );
    }

    private boolean isTerminalColumn(UUID columnId) {
        return boardColumnRepository.findById(columnId)
                .map(column -> Boolean.TRUE.equals(column.getIsTerminal()))
                .orElse(false);
    }

    private BigDecimal nextPosition(UUID columnId) {
        BigDecimal max = projectTaskRepository.findMaxBoardPosition(columnId);

        return (max == null ? BigDecimal.ZERO : max).add(POSITION_STEP);
    }

    private BigDecimal calculatePosition(UUID columnId, ProjectTask beforeTask, ProjectTask afterTask) {
        if (beforeTask != null && afterTask != null) {
            BigDecimal candidate = beforeTask.getBoardPosition()
                    .add(afterTask.getBoardPosition())
                    .divide(BigDecimal.valueOf(2), 15, RoundingMode.HALF_UP);

            if (candidate.subtract(beforeTask.getBoardPosition()).compareTo(MIN_POSITION_GAP) > 0
                    && afterTask.getBoardPosition().subtract(candidate).compareTo(MIN_POSITION_GAP) > 0) {
                return candidate;
            }

            renumberColumn(columnId);

            return findById(beforeTask.getId()).getBoardPosition()
                    .add(findById(afterTask.getId()).getBoardPosition())
                    .divide(BigDecimal.valueOf(2), 15, RoundingMode.HALF_UP);
        }

        if (afterTask != null) {
            if (afterTask.getBoardPosition().compareTo(MIN_POSITION_GAP) > 0) {
                return afterTask.getBoardPosition().divide(BigDecimal.valueOf(2), 15, RoundingMode.HALF_UP);
            }

            renumberColumn(columnId);

            return findById(afterTask.getId()).getBoardPosition()
                    .divide(BigDecimal.valueOf(2), 15, RoundingMode.HALF_UP);
        }

        if (beforeTask != null) {
            return beforeTask.getBoardPosition().add(POSITION_STEP);
        }

        return nextPosition(columnId);
    }

    private void renumberColumn(UUID columnId) {
        List<ProjectTask> tasks = projectTaskRepository.findAllByColumnIdOrderByBoardPositionAsc(columnId);

        BigDecimal position = POSITION_STEP;

        for (ProjectTask task : tasks) {
            task.setBoardPosition(position);
            position = position.add(POSITION_STEP);
        }

        projectTaskRepository.saveAll(tasks);
    }

    private ProjectTaskResponseDto toResponseDto(ProjectTask task, String projectCode) {
        ProjectTaskResponseDto response = projectTaskMapper.toResponseDto(task);

        response.setKey(projectCode + "-" + task.getTaskNumber());

        taskTypeRepository.findById(task.getTaskTypeId())
                .ifPresent(taskType -> response.setType(taskType.getCode()));

        boardColumnRepository.findById(task.getColumnId())
                .ifPresent(column -> response.setStatus(column.getCode()));

        response.setAssignees(
                taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(task.getId())
                        .stream()
                        .map(taskAssigneeMapper::toResponseDto)
                        .toList()
        );

        response.setChildrenCount(projectTaskRepository.countByParentTaskId(task.getId()));
        response.setCompletedChildrenCount(
                projectTaskRepository.countByParentTaskIdAndCompletedAtIsNotNull(task.getId())
        );
        response.setBlocked(isBlocked(task));

        return response;
    }

    private boolean isBlocked(ProjectTask task) {
        List<TaskRelation> blockers = taskRelationRepository
                .findAllByTargetTaskIdInAndRelationType(List.of(task.getId()), TaskRelationType.BLOCKS);

        if (blockers.isEmpty()) {
            return false;
        }

        Set<UUID> sourceIds = blockers.stream()
                .map(TaskRelation::getSourceTaskId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return projectTaskRepository.findAllById(sourceIds).stream()
                .anyMatch(source -> source.getCompletedAt() == null);
    }

    private UUID currentEmployeeId() {
        return employeeDirectoryService.getCurrentEmployee().getEmployeeId();
    }

    private String normalizeTypeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private Map<String, Object> payload(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<>();

        for (int index = 0; index + 1 < values.length; index += 2) {
            payload.put(String.valueOf(values[index]), values[index + 1]);
        }

        return payload;
    }

    private void addChange(Map<String, Object> changes, String field, Object current, Object requested) {
        if (requested != null && !Objects.equals(current, requested)) {
            changes.put(field, current);
        }
    }
}
