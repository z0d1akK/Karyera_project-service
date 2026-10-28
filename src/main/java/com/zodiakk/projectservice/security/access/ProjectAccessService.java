package com.zodiakk.projectservice.security.access;

import com.zodiakk.projectservice.board.repository.BoardColumnRepository;
import com.zodiakk.projectservice.board.repository.ProjectBoardRepository;
import com.zodiakk.projectservice.common.constants.SecurityConstants;
import com.zodiakk.projectservice.integration.exception.EmployeeReferenceNotFoundException;
import com.zodiakk.projectservice.integration.exception.EmployeeServiceUnavailableException;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectLinkRepository;
import com.zodiakk.projectservice.project.repository.ProjectMilestoneRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.security.util.SecurityUtils;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskAssignee;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.repository.TaskRelationRepository;
import com.zodiakk.projectservice.task.repository.TaskSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component("projectAccess")
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    private final ProjectAssignmentRepository projectAssignmentRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final TaskAssigneeRepository taskAssigneeRepository;

    private final ProjectMilestoneRepository projectMilestoneRepository;

    private final ProjectLinkRepository projectLinkRepository;

    private final ProjectBoardRepository projectBoardRepository;

    private final BoardColumnRepository boardColumnRepository;

    private final TaskRelationRepository taskRelationRepository;

    private final TaskSkillRepository taskSkillRepository;

    private final EmployeeDirectoryService employeeDirectoryService;

    public boolean isAdmin() {
        return SecurityUtils.hasRole(SecurityConstants.ADMIN_AUTHORITY);
    }

    public boolean isPrivileged() {
        return SecurityUtils.hasAnyRole(
                SecurityConstants.HR_AUTHORITY,
                SecurityConstants.ADMIN_AUTHORITY
        );
    }

    public boolean canViewAllProjects() {
        return SecurityUtils.hasAnyRole(
                SecurityConstants.MANAGER_AUTHORITY,
                SecurityConstants.HR_AUTHORITY,
                SecurityConstants.ADMIN_AUTHORITY
        );
    }

    public boolean canCreateProject() {
        return SecurityUtils.hasAnyRole(
                SecurityConstants.MANAGER_AUTHORITY,
                SecurityConstants.ADMIN_AUTHORITY
        );
    }

    public boolean canManageCatalogs() {
        return isAdmin();
    }

    public boolean canViewProject(UUID projectId) {
        if (canViewAllProjects()) {
            return true;
        }

        return currentEmployeeId()
                .map(employeeId -> isProjectParticipant(projectId, employeeId))
                .orElse(false);
    }

    public boolean canEditProject(UUID projectId) {
        if (isAdmin()) {
            return true;
        }

        return currentEmployeeId()
                .map(employeeId -> isProjectManager(projectId, employeeId))
                .orElse(false);
    }

    public boolean canManageProjectMembers(UUID projectId) {
        if (isPrivileged()) {
            return true;
        }

        return currentEmployeeId()
                .map(employeeId -> isProjectManager(projectId, employeeId))
                .orElse(false);
    }

    public boolean canViewTask(UUID taskId) {
        return findTask(taskId)
                .map(task -> canViewProject(task.getProjectId()) || isTaskAssignee(taskId))
                .orElse(false);
    }

    public boolean canEditTask(UUID taskId) {
        Optional<ProjectTask> task = findTask(taskId);

        if (task.isEmpty()) {
            return false;
        }

        if (isAdmin()) {
            return true;
        }

        Optional<UUID> employeeId = currentEmployeeId();

        if (employeeId.isEmpty()) {
            return false;
        }

        if (isProjectManager(task.get().getProjectId(), employeeId.get())) {
            return true;
        }

        return isTaskAssignee(taskId, employeeId.get());
    }

    public boolean canEditMilestone(UUID milestoneId) {
        return projectMilestoneRepository.findById(milestoneId)
                .map(milestone -> canEditProject(milestone.getProjectId()))
                .orElse(false);
    }

    public boolean canEditLink(UUID linkId) {
        return projectLinkRepository.findById(linkId)
                .map(link -> canEditProject(link.getProjectId()))
                .orElse(false);
    }

    public boolean canEditBoard(UUID boardId) {
        return projectBoardRepository.findById(boardId)
                .map(board -> canEditProject(board.getProjectId()))
                .orElse(false);
    }

    public boolean canEditBoardColumn(UUID columnId) {
        return boardColumnRepository.findById(columnId)
                .flatMap(column -> projectBoardRepository.findById(column.getBoardId()))
                .map(board -> canEditProject(board.getProjectId()))
                .orElse(false);
    }

    public boolean canEditTaskRelation(UUID relationId) {
        return taskRelationRepository.findById(relationId)
                .map(relation -> canEditTask(relation.getSourceTaskId()))
                .orElse(false);
    }

    public boolean canEditTaskSkill(UUID taskSkillId) {
        return taskSkillRepository.findById(taskSkillId)
                .map(skill -> canEditTask(skill.getTaskId()))
                .orElse(false);
    }

    private boolean isProjectParticipant(UUID projectId, UUID employeeId) {
        return projectRepository.findById(projectId)
                .map(project -> manages(project, employeeId)
                        || projectAssignmentRepository.existsByProjectIdAndEmployeeId(projectId, employeeId))
                .orElse(false);
    }

    private boolean isProjectManager(UUID projectId, UUID employeeId) {
        return projectRepository.findById(projectId)
                .map(project -> manages(project, employeeId))
                .orElse(false);
    }

    private boolean manages(Project project, UUID employeeId) {
        return employeeId.equals(project.getProjectManagerId())
                || employeeId.equals(project.getDeliveryManagerId());
    }

    private boolean isTaskAssignee(UUID taskId) {
        return currentEmployeeId()
                .map(employeeId -> isTaskAssignee(taskId, employeeId))
                .orElse(false);
    }

    private boolean isTaskAssignee(UUID taskId, UUID employeeId) {
        List<TaskAssignee> assignees =
                taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(taskId);

        if (assignees.isEmpty()) {
            return false;
        }

        Set<UUID> assignmentIds = assignees.stream()
                .map(TaskAssignee::getAssignmentId)
                .collect(Collectors.toSet());

        return projectAssignmentRepository.findAllById(assignmentIds).stream()
                .map(ProjectAssignment::getEmployeeId)
                .anyMatch(employeeId::equals);
    }

    private Optional<ProjectTask> findTask(UUID taskId) {
        return projectTaskRepository.findById(taskId);
    }

    private Optional<UUID> currentEmployeeId() {
        try {
            return employeeDirectoryService.findCurrentEmployeeId();
        } catch (EmployeeReferenceNotFoundException | EmployeeServiceUnavailableException ex) {
            return Optional.empty();
        }
    }
}
