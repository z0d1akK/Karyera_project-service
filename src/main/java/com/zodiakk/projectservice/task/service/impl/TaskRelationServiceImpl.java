package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.common.enums.TaskRelationType;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.task.dto.request.action.TaskRelationCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskRelationResponseDto;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskRelation;
import com.zodiakk.projectservice.task.exception.relation.CrossProjectRelationException;
import com.zodiakk.projectservice.task.exception.relation.TaskDependencyCycleException;
import com.zodiakk.projectservice.task.exception.relation.TaskRelationAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.relation.TaskRelationNotFoundException;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.mapper.TaskRelationMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskRelationRepository;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import com.zodiakk.projectservice.task.service.TaskRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskRelationServiceImpl implements TaskRelationService {

    private static final String ACTION_CREATED = "TASK_RELATION_CREATED";

    private static final String ACTION_DELETED = "TASK_RELATION_DELETED";

    private final TaskRelationRepository taskRelationRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final TaskRelationMapper taskRelationMapper;

    private final TaskActivityService taskActivityService;

    private final EmployeeDirectoryService employeeDirectoryService;

    @Override
    @PreAuthorize("@projectAccess.canViewTask(#taskId)")
    public List<TaskRelationResponseDto> findAll(UUID taskId) {
        if (!projectTaskRepository.existsById(taskId)) {
            throw new ProjectTaskNotFoundException(taskId);
        }

        return taskRelationRepository.findAllBySourceTaskId(taskId)
                .stream()
                .map(taskRelationMapper::toResponseDto)
                .toList();
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTask(#sourceTaskId)")
    @Transactional
    public TaskRelationResponseDto create(UUID sourceTaskId, TaskRelationCreateRequestDto request) {
        ProjectTask source = findTask(sourceTaskId);
        ProjectTask target = findTask(request.getTargetTaskId());

        if (Objects.equals(source.getId(), target.getId())) {
            throw new TaskRelationAlreadyExistsException();
        }

        if (!source.getProjectId().equals(target.getProjectId())) {
            throw new CrossProjectRelationException();
        }

        if (taskRelationRepository.existsBySourceTaskIdAndTargetTaskIdAndRelationType(
                source.getId(),
                target.getId(),
                request.getRelationType()
        )) {
            throw new TaskRelationAlreadyExistsException();
        }

        if (request.getRelationType() == TaskRelationType.BLOCKS
                && hasPath(target.getId(), source.getId(), new HashSet<>())) {
            throw new TaskDependencyCycleException();
        }

        UUID createdBy = employeeDirectoryService.getCurrentEmployee().getEmployeeId();

        TaskRelation relation = TaskRelation.builder()
                .sourceTaskId(source.getId())
                .targetTaskId(target.getId())
                .relationType(request.getRelationType())
                .createdBy(createdBy)
                .build();

        TaskRelation saved = taskRelationRepository.save(relation);

        taskActivityService.record(
                source.getId(),
                createdBy,
                ACTION_CREATED,
                changes(saved)
        );

        return taskRelationMapper.toResponseDto(saved);
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTaskRelation(#id)")
    @Transactional
    public void delete(UUID id) {
        TaskRelation relation = taskRelationRepository.findById(id)
                .orElseThrow(() -> new TaskRelationNotFoundException(id));

        taskRelationRepository.delete(relation);

        taskActivityService.record(
                relation.getSourceTaskId(),
                employeeDirectoryService.getCurrentEmployee().getEmployeeId(),
                ACTION_DELETED,
                changes(relation)
        );
    }

    private ProjectTask findTask(UUID id) {
        return projectTaskRepository.findById(id)
                .orElseThrow(() -> new ProjectTaskNotFoundException(id));
    }

    private boolean hasPath(UUID from, UUID to, Set<UUID> visited) {
        if (from.equals(to)) {
            return true;
        }

        if (!visited.add(from)) {
            return false;
        }

        for (TaskRelation relation : taskRelationRepository
                .findAllBySourceTaskIdAndRelationType(from, TaskRelationType.BLOCKS)) {
            if (hasPath(relation.getTargetTaskId(), to, visited)) {
                return true;
            }
        }

        return false;
    }

    private Map<String, Object> changes(TaskRelation relation) {
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("targetTaskId", relation.getTargetTaskId());
        changes.put("relationType", relation.getRelationType());

        return changes;
    }
}
