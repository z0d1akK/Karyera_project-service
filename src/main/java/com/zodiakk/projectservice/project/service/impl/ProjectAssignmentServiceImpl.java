package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.common.constants.EventConstants;
import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.integration.exception.EmployeeReferenceInactiveException;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.outbox.service.OutboxEventService;
import com.zodiakk.projectservice.project.dto.request.action.ProjectAssignmentEndRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectAssignmentCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectAssignmentFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectAssignmentUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectAssignmentResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;
import com.zodiakk.projectservice.project.entity.ProjectRole;
import com.zodiakk.projectservice.project.exception.assignment.InvalidAllocationPercentException;
import com.zodiakk.projectservice.project.exception.assignment.InvalidAssignmentDatesException;
import com.zodiakk.projectservice.project.exception.assignment.ProjectAssignmentAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.assignment.ProjectAssignmentNotFoundException;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleInactiveException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectAssignmentMapper;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.project.repository.ProjectRoleRepository;
import com.zodiakk.projectservice.project.repository.specification.projectAssignment.ProjectAssignmentPageableFactory;
import com.zodiakk.projectservice.project.repository.specification.projectAssignment.ProjectAssignmentSpecification;
import com.zodiakk.projectservice.project.service.ProjectAssignmentService;
import com.zodiakk.projectservice.security.access.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAssignmentServiceImpl implements ProjectAssignmentService {

    private static final BigDecimal MAX_ALLOCATION = BigDecimal.valueOf(100);

    private final ProjectAssignmentRepository projectAssignmentRepository;

    private final ProjectRepository projectRepository;

    private final ProjectRoleRepository projectRoleRepository;

    private final ProjectAssignmentMapper projectAssignmentMapper;

    private final OutboxEventService outboxEventService;

    private final EmployeeDirectoryService employeeDirectoryService;

    private final ProjectAccessService projectAccessService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public Page<ProjectAssignmentResponseDto> findAll(
            ProjectAssignmentFilterDto filter,
            PageRequestDto pageRequest
    ) {
        if (filter.getProjectId() != null && !projectAccessService.canViewProject(filter.getProjectId())) {
            return Page.empty();
        }

        if (filter.getProjectId() == null && !projectAccessService.canViewAllProjects()) {
            UUID employeeId = employeeDirectoryService.getCurrentEmployee().getEmployeeId();
            filter.setEmployeeId(employeeId);
        }

        Pageable pageable = ProjectAssignmentPageableFactory.create(pageRequest);

        Page<ProjectAssignment> page = projectAssignmentRepository.findAll(
                ProjectAssignmentSpecification.filter(filter),
                pageable
        );

        Map<UUID, EmployeeReferenceDto> employees = employeeDirectoryService.resolveAll(
                page.getContent().stream()
                        .map(ProjectAssignment::getEmployeeId)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );

        Map<UUID, ProjectRole> roles = projectRoleRepository.findAllById(
                        page.getContent().stream()
                                .map(ProjectAssignment::getProjectRoleId)
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                ).stream()
                .collect(Collectors.toMap(ProjectRole::getId, Function.identity()));

        return page.map(assignment -> toResponseDto(assignment, employees, roles));
    }

    @Override
    @PreAuthorize("@projectAccess.canManageProjectMembers(#projectId)")
    @Transactional
    public ProjectAssignmentResponseDto create(UUID projectId, ProjectAssignmentCreateRequestDto request) {
        request.setProjectId(projectId);

        if (!projectRepository.existsById(request.getProjectId())) {
            throw new ProjectNotFoundException(request.getProjectId());
        }

        ProjectRole role = requireActiveRole(request.getProjectRoleId());

        EmployeeReferenceDto employee = employeeDirectoryService.requireById(request.getEmployeeId());

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new EmployeeReferenceInactiveException(request.getEmployeeId());
        }

        if (projectAssignmentRepository.existsByProjectIdAndEmployeeIdAndEndDateIsNull(
                request.getProjectId(),
                request.getEmployeeId()
        )) {
            throw new ProjectAssignmentAlreadyExistsException(
                    request.getEmployeeId(),
                    request.getProjectId()
            );
        }

        validateDates(request.getStartDate(), request.getEndDate());
        validateAllocation(request.getAllocationPercent());

        ProjectAssignment assignment = projectAssignmentMapper.toEntity(request);
        ProjectAssignment saved = projectAssignmentRepository.save(assignment);

        outboxEventService.saveEvent(
                saved.getId(),
                EventConstants.AGGREGATE_PROJECT_ASSIGNMENT,
                EventConstants.PROJECT_ASSIGNMENT_CREATED,
                EventConstants.DEFAULT_EVENT_VERSION,
                assignmentPayload(saved)
        );

        return toResponseDto(
                saved,
                Map.of(employee.getEmployeeId(), employee),
                Map.of(role.getId(), role)
        );
    }

    @Override
    @PreAuthorize("@projectAccess.canManageProjectMembers(#id)")
    @Transactional
    public ProjectAssignmentResponseDto update(UUID id, ProjectAssignmentUpdateRequestDto request) {
        ProjectAssignment assignment = findById(id);

        ProjectRole role = assignment.getProjectRoleId() != null
                ? requireRole(assignment.getProjectRoleId())
                : null;

        if (request.getProjectRoleId() != null) {
            role = requireActiveRole(request.getProjectRoleId());
        }

        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : assignment.getStartDate();
        LocalDate endDate = request.getEndDate() != null
                ? request.getEndDate()
                : assignment.getEndDate();

        validateDates(startDate, endDate);
        validateAllocation(request.getAllocationPercent());

        projectAssignmentMapper.updateEntity(request, assignment);

        ProjectAssignment saved = projectAssignmentRepository.save(assignment);

        return toResponseDto(
                saved,
                employeeDirectoryService.resolveAll(List.of(saved.getEmployeeId())),
                role != null ? Map.of(role.getId(), role) : Map.of()
        );
    }

    @Override
    @PreAuthorize("@projectAccess.canManageProjectMembers(#id)")
    @Transactional
    public ProjectAssignmentResponseDto end(UUID id, ProjectAssignmentEndRequestDto request) {
        ProjectAssignment assignment = projectAssignmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProjectAssignmentNotFoundException(id));

        if (assignment.getEndDate() != null) {
            return toResponseDto(
                    assignment,
                    employeeDirectoryService.resolveAll(List.of(assignment.getEmployeeId())),
                    Map.of()
            );
        }

        LocalDate endDate = request.getEndDate() != null
                ? request.getEndDate()
                : LocalDate.now();

        validateDates(assignment.getStartDate(), endDate);

        assignment.setEndDate(endDate);
        ProjectAssignment saved = projectAssignmentRepository.save(assignment);

        outboxEventService.saveEvent(
                saved.getId(),
                EventConstants.AGGREGATE_PROJECT_ASSIGNMENT,
                EventConstants.PROJECT_ASSIGNMENT_ENDED,
                EventConstants.DEFAULT_EVENT_VERSION,
                assignmentPayload(saved)
        );

        return toResponseDto(
                saved,
                employeeDirectoryService.resolveAll(List.of(saved.getEmployeeId())),
                Map.of()
        );
    }

    private ProjectAssignment findById(UUID id) {
        return projectAssignmentRepository.findById(id)
                .orElseThrow(() -> new ProjectAssignmentNotFoundException(id));
    }

    private ProjectRole requireRole(UUID id) {
        return projectRoleRepository.findById(id)
                .orElseThrow(() -> new ProjectRoleNotFoundException(id));
    }

    private ProjectRole requireActiveRole(UUID id) {
        ProjectRole role = requireRole(id);

        if (!Boolean.TRUE.equals(role.getActive())) {
            throw new ProjectRoleInactiveException(id);
        }

        return role;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidAssignmentDatesException();
        }
    }

    private void validateAllocation(BigDecimal allocationPercent) {
        if (allocationPercent == null) {
            return;
        }

        if (allocationPercent.compareTo(BigDecimal.ZERO) < 0
                || allocationPercent.compareTo(MAX_ALLOCATION) > 0) {
            throw new InvalidAllocationPercentException(allocationPercent);
        }
    }

    private ProjectAssignmentResponseDto toResponseDto(
            ProjectAssignment assignment,
            Map<UUID, EmployeeReferenceDto> employees,
            Map<UUID, ProjectRole> roles
    ) {
        ProjectAssignmentResponseDto response = projectAssignmentMapper.toResponseDto(assignment);

        EmployeeReferenceDto employee = employees.get(assignment.getEmployeeId());
        if (employee != null) {
            response.setEmployeeFullName(employee.getFullName());
        }

        ProjectRole role = roles.get(assignment.getProjectRoleId());
        if (role != null) {
            response.setProjectRoleName(role.getName());
        }

        return response;
    }

    private Map<String, Object> assignmentPayload(ProjectAssignment assignment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assignmentId", assignment.getId());
        payload.put("projectId", assignment.getProjectId());
        payload.put("employeeId", assignment.getEmployeeId());
        payload.put("projectRoleId", assignment.getProjectRoleId());
        payload.put("startDate", assignment.getStartDate());
        payload.put("endDate", assignment.getEndDate());
        payload.put("allocationPercent", assignment.getAllocationPercent());

        return payload;
    }
}
