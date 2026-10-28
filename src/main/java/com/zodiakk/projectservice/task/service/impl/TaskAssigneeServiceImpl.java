package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.common.constants.EventConstants;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.outbox.service.OutboxEventService;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.task.dto.request.action.TaskAssigneesUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskAssigneeResponseDto;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskAssignee;
import com.zodiakk.projectservice.task.exception.assignee.InvalidTaskAssigneeException;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.exception.task.TaskArchivedException;
import com.zodiakk.projectservice.task.mapper.TaskAssigneeMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import com.zodiakk.projectservice.task.service.TaskAssigneeService;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAssigneeServiceImpl implements TaskAssigneeService {

    private static final String ACTION_ASSIGNEES_CHANGED = "TASK_ASSIGNEES_CHANGED";

    private final TaskAssigneeRepository taskAssigneeRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final ProjectAssignmentRepository projectAssignmentRepository;

    private final TaskAssigneeMapper taskAssigneeMapper;

    private final TaskActivityService taskActivityService;

    private final OutboxEventService outboxEventService;

    private final EmployeeDirectoryService employeeDirectoryService;

    @Override
    @PreAuthorize("@projectAccess.canViewTask(#taskId)")
    public List<TaskAssigneeResponseDto> findAll(UUID taskId) {
        if (!projectTaskRepository.existsById(taskId)) {
            throw new ProjectTaskNotFoundException(taskId);
        }

        return activeAssignees(taskId);
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTask(#taskId)")
    @Transactional
    public List<TaskAssigneeResponseDto> replace(UUID taskId, TaskAssigneesUpdateRequestDto request) {
        ProjectTask task = projectTaskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ProjectTaskNotFoundException(taskId));

        if (task.getArchivedAt() != null) {
            throw new TaskArchivedException(taskId);
        }

        verifyVersion(request.getExpectedVersion(), task.getVersion());

        Set<UUID> requestedIds = new LinkedHashSet<>(request.getAssigneeIds());

        validateAssignments(task, requestedIds);

        List<TaskAssignee> active = taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(taskId);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (TaskAssignee assignee : active) {
            if (!requestedIds.contains(assignee.getAssignmentId())) {
                assignee.setUnassignedAt(now);
            }
        }

        Set<UUID> activeIds = active.stream()
                .map(TaskAssignee::getAssignmentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (UUID assignmentId : requestedIds) {
            if (!activeIds.contains(assignmentId)) {
                active.add(TaskAssignee.builder()
                        .taskId(taskId)
                        .assignmentId(assignmentId)
                        .assignedAt(now)
                        .build());
            }
        }

        taskAssigneeRepository.saveAll(active);

        outboxEventService.saveEvent(
                taskId,
                EventConstants.AGGREGATE_PROJECT_TASK,
                EventConstants.PROJECT_TASK_ASSIGNEES_CHANGED,
                EventConstants.DEFAULT_EVENT_VERSION,
                payload(task, requestedIds)
        );

        taskActivityService.record(
                taskId,
                employeeDirectoryService.getCurrentEmployee().getEmployeeId(),
                ACTION_ASSIGNEES_CHANGED,
                Map.of("assignmentIds", List.copyOf(requestedIds))
        );

        return activeAssignees(taskId);
    }

    private void validateAssignments(ProjectTask task, Set<UUID> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return;
        }

        List<ProjectAssignment> assignments = projectAssignmentRepository.findAllById(assignmentIds);
        LocalDate today = LocalDate.now();

        if (assignments.size() != assignmentIds.size()) {
            throw new InvalidTaskAssigneeException(assignmentIds.iterator().next());
        }

        for (ProjectAssignment assignment : assignments) {
            boolean belongsToProject = task.getProjectId().equals(assignment.getProjectId());
            boolean active = assignment.getEndDate() == null || !assignment.getEndDate().isBefore(today);

            if (!belongsToProject || !active) {
                throw new InvalidTaskAssigneeException(assignment.getId());
            }
        }
    }

    private List<TaskAssigneeResponseDto> activeAssignees(UUID taskId) {
        return taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(taskId)
                .stream()
                .sorted(Comparator.comparing(TaskAssignee::getAssignedAt))
                .map(taskAssigneeMapper::toResponseDto)
                .toList();
    }

    private void verifyVersion(Long expectedVersion, Long actualVersion) {
        if (expectedVersion != null && !expectedVersion.equals(actualVersion)) {
            throw new ObjectOptimisticLockingFailureException(ProjectTask.class, expectedVersion);
        }
    }

    private Map<String, Object> payload(ProjectTask task, Set<UUID> assignmentIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("projectId", task.getProjectId());
        payload.put("assignmentIds", List.copyOf(assignmentIds));

        return payload;
    }
}
