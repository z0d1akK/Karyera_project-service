package com.zodiakk.projectservice.internal.service.impl;

import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.internal.dto.ProjectHistoryEntryDto;
import com.zodiakk.projectservice.internal.dto.ProjectHistoryResponseDto;
import com.zodiakk.projectservice.internal.dto.TaskStatisticsResponseDto;
import com.zodiakk.projectservice.internal.dto.WorkEligibilityReasonCode;
import com.zodiakk.projectservice.internal.dto.WorkEligibilityResponseDto;
import com.zodiakk.projectservice.internal.service.InternalProjectService;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;
import com.zodiakk.projectservice.project.entity.ProjectRole;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.project.repository.ProjectRoleRepository;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskType;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalProjectServiceImpl implements InternalProjectService {

    private static final LocalDate MIN_DATE = LocalDate.of(1970, 1, 1);

    private final ProjectRepository projectRepository;

    private final ProjectRoleRepository projectRoleRepository;

    private final ProjectAssignmentRepository projectAssignmentRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final TaskTypeRepository taskTypeRepository;

    @Override
    public WorkEligibilityResponseDto getWorkEligibility(
            UUID projectId,
            UUID employeeId,
            UUID taskId,
            LocalDate workDate
    ) {
        Project project = projectRepository.findById(projectId).orElse(null);

        if (project == null) {
            return new WorkEligibilityResponseDto(
                    false,
                    projectId,
                    taskId,
                    employeeId,
                    workDate,
                    null,
                    false,
                    null,
                    false,
                    null,
                    WorkEligibilityReasonCode.PROJECT_NOT_FOUND
            );
        }

        boolean projectActive = project.getStatus() == ProjectStatus.ACTIVE;
        boolean projectAvailable = isProjectAvailableOnDate(project, workDate);

        ProjectAssignment activeAssignment = projectAssignmentRepository
                .findActiveOnDate(employeeId, workDate)
                .stream()
                .filter(assignment -> projectId.equals(assignment.getProjectId()))
                .findFirst()
                .orElse(null);

        boolean assignmentActiveOnDate = activeAssignment != null;

        ProjectTask task = taskId != null
                ? projectTaskRepository.findById(taskId).orElse(null)
                : null;

        Boolean taskBelongsToProject = null;
        Boolean taskArchived = null;

        if (taskId != null) {
            taskBelongsToProject = task != null && projectId.equals(task.getProjectId());
            taskArchived = task != null ? task.getArchivedAt() != null : null;
        }

        WorkEligibilityReasonCode reasonCode = resolveReasonCode(
                taskId,
                task,
                projectId,
                projectAvailable,
                assignmentActiveOnDate
        );

        return new WorkEligibilityResponseDto(
                reasonCode == null,
                projectId,
                taskId,
                employeeId,
                workDate,
                activeAssignment != null ? activeAssignment.getId() : null,
                projectActive,
                taskBelongsToProject,
                assignmentActiveOnDate,
                taskArchived,
                reasonCode
        );
    }

    private WorkEligibilityReasonCode resolveReasonCode(
            UUID taskId,
            ProjectTask task,
            UUID projectId,
            boolean projectAvailable,
            boolean assignmentActiveOnDate
    ) {
        if (taskId != null && task == null) {
            return WorkEligibilityReasonCode.TASK_NOT_FOUND;
        }

        if (taskId != null && !projectId.equals(task.getProjectId())) {
            return WorkEligibilityReasonCode.TASK_PROJECT_MISMATCH;
        }

        if (!projectAvailable) {
            return WorkEligibilityReasonCode.PROJECT_NOT_AVAILABLE_FOR_TIME_TRACKING;
        }

        if (!assignmentActiveOnDate) {
            return WorkEligibilityReasonCode.NO_ACTIVE_PROJECT_ASSIGNMENT;
        }

        if (task != null && task.getArchivedAt() != null) {
            return WorkEligibilityReasonCode.TASK_ARCHIVED;
        }

        return null;
    }

    /**
     * Time may be recorded for an active project, and for a closed project when the work date
     * falls into its historical period. The current status alone must not block historical time.
     */
    private boolean isProjectAvailableOnDate(Project project, LocalDate workDate) {
        if (project.getStatus() == ProjectStatus.ACTIVE) {
            return true;
        }

        if (project.getStatus() != ProjectStatus.COMPLETED
                && project.getStatus() != ProjectStatus.CANCELLED) {
            return false;
        }

        if (project.getEndDate() == null || workDate.isAfter(project.getEndDate())) {
            return false;
        }

        return project.getStartDate() == null || !workDate.isBefore(project.getStartDate());
    }

    @Override
    public ProjectHistoryResponseDto getProjectHistory(UUID employeeId) {
        List<ProjectAssignment> assignments = projectAssignmentRepository.findAllByEmployeeId(employeeId);

        Map<UUID, Project> projects = loadByIds(
                assignments.stream().map(ProjectAssignment::getProjectId).collect(Collectors.toSet()),
                projectRepository::findAllById,
                Project::getId
        );

        Map<UUID, ProjectRole> roles = loadByIds(
                assignments.stream().map(ProjectAssignment::getProjectRoleId).collect(Collectors.toSet()),
                projectRoleRepository::findAllById,
                ProjectRole::getId
        );

        List<ProjectHistoryEntryDto> entries = assignments.stream()
                .sorted((first, second) -> {
                    if (first.getStartDate() == null || second.getStartDate() == null) {
                        return 0;
                    }
                    return second.getStartDate().compareTo(first.getStartDate());
                })
                .map(assignment -> {
                    Project project = projects.get(assignment.getProjectId());
                    ProjectRole role = roles.get(assignment.getProjectRoleId());

                    return new ProjectHistoryEntryDto(
                            assignment.getProjectId(),
                            project != null ? project.getCode() : null,
                            project != null ? project.getName() : null,
                            project != null ? project.getStatus() : null,
                            assignment.getProjectRoleId(),
                            role != null ? role.getName() : null,
                            assignment.getStartDate(),
                            assignment.getEndDate(),
                            assignment.getAllocationPercent(),
                            assignment.getEndDate() == null
                    );
                })
                .toList();

        return new ProjectHistoryResponseDto(employeeId, entries);
    }

    @Override
    public TaskStatisticsResponseDto getTaskStatistics(
            UUID employeeId,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        LocalDate end = periodEnd != null ? periodEnd : LocalDate.now();
        LocalDate start = periodStart != null ? periodStart : MIN_DATE;

        if (end.isBefore(start)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }

        LocalDate periodFrom = start;
        LocalDate periodTo = end;

        List<ProjectAssignment> assignments = projectAssignmentRepository
                .findAllByEmployeeId(employeeId)
                .stream()
                .filter(assignment -> overlaps(assignment, periodFrom, periodTo))
                .toList();

        Set<UUID> projectIds = assignments.stream()
                .map(ProjectAssignment::getProjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UUID> assignmentIds = assignments.stream()
                .map(ProjectAssignment::getId)
                .toList();

        OffsetDateTime from = periodFrom.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = periodTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<ProjectTask> completed = assignmentIds.isEmpty()
                ? List.of()
                : projectTaskRepository.findCompletedByAssignmentIds(assignmentIds, from, to);

        Map<UUID, String> typeCodes = loadByIds(
                completed.stream().map(ProjectTask::getTaskTypeId).collect(Collectors.toSet()),
                taskTypeRepository::findAllById,
                TaskType::getId
        ).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getCode()));

        Map<String, Long> completedByType = new TreeMap<>();
        Map<String, Long> completedByComplexity = new TreeMap<>();

        for (ProjectTask task : completed) {
            String type = typeCodes.getOrDefault(task.getTaskTypeId(), "UNKNOWN");
            completedByType.merge(type, 1L, Long::sum);

            String complexity = task.getComplexity() != null ? task.getComplexity().name() : "UNSPECIFIED";
            completedByComplexity.merge(complexity, 1L, Long::sum);
        }

        return new TaskStatisticsResponseDto(
                employeeId,
                periodFrom,
                periodTo,
                completed.size(),
                new LinkedHashMap<>(completedByType),
                new LinkedHashMap<>(completedByComplexity),
                projectIds.size()
        );
    }

    private boolean overlaps(ProjectAssignment assignment, LocalDate start, LocalDate end) {
        boolean startedBeforeEnd = assignment.getStartDate() == null || !assignment.getStartDate().isAfter(end);
        boolean endsAfterStart = assignment.getEndDate() == null || !assignment.getEndDate().isBefore(start);

        return startedBeforeEnd && endsAfterStart;
    }

    private <T> Map<UUID, T> loadByIds(
            Set<UUID> ids,
            Function<Set<UUID>, List<T>> loader,
            Function<T, UUID> idExtractor
    ) {
        Set<UUID> nonNullIds = ids.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (nonNullIds.isEmpty()) {
            return Map.of();
        }

        return loader.apply(nonNullIds).stream()
                .collect(Collectors.toMap(idExtractor, Function.identity(), (first, second) -> first));
    }
}
