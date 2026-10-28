package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.common.cache.CacheNames;
import com.zodiakk.projectservice.common.constants.EventConstants;
import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.outbox.service.OutboxEventService;
import com.zodiakk.projectservice.project.dto.request.action.ProjectStatusUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectResponseDto;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.exception.project.InvalidProjectDatesException;
import com.zodiakk.projectservice.project.exception.project.InvalidProjectStatusTransitionException;
import com.zodiakk.projectservice.project.exception.project.ProjectCodeAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectMapper;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.project.repository.specification.project.ProjectPageableFactory;
import com.zodiakk.projectservice.project.repository.specification.project.ProjectSpecification;
import com.zodiakk.projectservice.project.service.ProjectService;
import com.zodiakk.projectservice.security.access.ProjectAccessService;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private static final Map<ProjectStatus, Set<ProjectStatus>> ALLOWED_TRANSITIONS = Map.of(
            ProjectStatus.PLANNED, Set.of(ProjectStatus.ACTIVE, ProjectStatus.CANCELLED),
            ProjectStatus.ACTIVE, Set.of(ProjectStatus.ON_HOLD, ProjectStatus.COMPLETED, ProjectStatus.CANCELLED),
            ProjectStatus.ON_HOLD, Set.of(ProjectStatus.ACTIVE, ProjectStatus.CANCELLED),
            ProjectStatus.COMPLETED, Set.of(ProjectStatus.ACTIVE),
            ProjectStatus.CANCELLED, Set.of(ProjectStatus.PLANNED)
    );

    private final ProjectRepository projectRepository;

    private final ProjectAssignmentRepository projectAssignmentRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final ProjectMapper projectMapper;

    private final OutboxEventService outboxEventService;

    private final EmployeeDirectoryService employeeDirectoryService;

    private final ProjectAccessService projectAccessService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public Page<ProjectResponseDto> findAll(ProjectFilterDto filter, PageRequestDto pageRequest) {
        Specification<Project> specification = ProjectSpecification.filter(filter);

        if (!projectAccessService.canViewAllProjects()) {
            UUID employeeId = employeeDirectoryService.getCurrentEmployee().getEmployeeId();
            specification = specification.and(ProjectSpecification.visibleTo(employeeId));
        }

        Pageable pageable = ProjectPageableFactory.create(pageRequest);

        Page<Project> page = projectRepository.findAll(specification, pageable);

        Map<UUID, EmployeeReferenceDto> employees = resolveEmployees(page.getContent());

        return page.map(project -> toResponseDto(project, employees));
    }

    @Override
    @PreAuthorize("@projectAccess.canViewProject(#id)")
    @Cacheable(cacheNames = CacheNames.PROJECTS, key = "#id")
    public ProjectResponseDto getById(UUID id) {
        Project project = findById(id);

        return toResponseDto(project, resolveEmployees(List.of(project)));
    }

    @Override
    @PreAuthorize("@projectAccess.canCreateProject()")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PROJECTS, allEntries = true)
    public ProjectResponseDto create(ProjectCreateRequestDto request) {
        validateCodeUniqueness(request.getCode());
        validateDates(request.getStartDate(), request.getEndDate());
        validateManagers(request.getProjectManagerId(), request.getDeliveryManagerId());

        Project project = projectMapper.toEntity(request);
        project.setStatus(ProjectStatus.PLANNED);

        Project saved = projectRepository.save(project);

        outboxEventService.saveEvent(
                saved.getId(),
                EventConstants.AGGREGATE_PROJECT,
                EventConstants.PROJECT_CREATED,
                EventConstants.DEFAULT_EVENT_VERSION,
                projectPayload(saved)
        );

        return toResponseDto(saved, resolveEmployees(List.of(saved)));
    }

    @Override
    @PreAuthorize("@projectAccess.canEditProject(#id)")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PROJECTS, allEntries = true)
    public ProjectResponseDto update(UUID id, ProjectUpdateRequestDto request) {
        Project project = findById(id);
        ProjectStatus previousStatus = project.getStatus();

        if (request.getCode() != null
                && !request.getCode().isBlank()
                && !request.getCode().equalsIgnoreCase(project.getCode())) {
            validateCodeUniqueness(request.getCode());
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : project.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : project.getEndDate();

        validateDates(startDate, endDate);
        validateManagers(request.getProjectManagerId(), request.getDeliveryManagerId());

        projectMapper.updateEntity(request, project);

        Project saved = projectRepository.save(project);

        if (request.getStatus() != null && request.getStatus() != previousStatus) {
            validateTransition(previousStatus, request.getStatus());

            outboxEventService.saveEvent(
                    saved.getId(),
                    EventConstants.AGGREGATE_PROJECT,
                    EventConstants.PROJECT_STATUS_CHANGED,
                    EventConstants.DEFAULT_EVENT_VERSION,
                    statusPayload(saved, previousStatus)
            );
        }

        return toResponseDto(saved, resolveEmployees(List.of(saved)));
    }

    @Override
    @PreAuthorize("@projectAccess.canEditProject(#id)")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PROJECTS, allEntries = true)
    public ProjectResponseDto changeStatus(UUID id, ProjectStatusUpdateRequestDto request) {
        Project project = projectRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        verifyVersion(request.getExpectedVersion(), project.getVersion());

        ProjectStatus previousStatus = project.getStatus();

        if (previousStatus != request.getStatus()) {
            validateTransition(previousStatus, request.getStatus());

            project.setStatus(request.getStatus());

            Project saved = projectRepository.save(project);

            outboxEventService.saveEvent(
                    saved.getId(),
                    EventConstants.AGGREGATE_PROJECT,
                    EventConstants.PROJECT_STATUS_CHANGED,
                    EventConstants.DEFAULT_EVENT_VERSION,
                    statusPayload(saved, previousStatus)
            );

            return toResponseDto(saved, resolveEmployees(List.of(saved)));
        }

        return toResponseDto(project, resolveEmployees(List.of(project)));
    }

    private Project findById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private Map<UUID, EmployeeReferenceDto> resolveEmployees(List<Project> projects) {
        Set<UUID> employeeIds = new LinkedHashSet<>();

        for (Project project : projects) {
            if (project.getProjectManagerId() != null) {
                employeeIds.add(project.getProjectManagerId());
            }
            if (project.getDeliveryManagerId() != null) {
                employeeIds.add(project.getDeliveryManagerId());
            }
        }

        return employeeDirectoryService.resolveAll(employeeIds);
    }

    private ProjectResponseDto toResponseDto(
            Project project,
            Map<UUID, EmployeeReferenceDto> employees
    ) {
        ProjectResponseDto response = projectMapper.toResponseDto(project);

        if (project.getProjectManagerId() != null) {
            response.setProjectManager(employees.get(project.getProjectManagerId()));
        }
        if (project.getDeliveryManagerId() != null) {
            response.setDeliveryManager(employees.get(project.getDeliveryManagerId()));
        }

        response.setMemberCount(projectAssignmentRepository.countByProjectId(project.getId()));
        response.setTaskCount(
                projectTaskRepository.countByProjectIdAndArchivedAtIsNull(project.getId())
        );

        return response;
    }

    private void validateCodeUniqueness(String code) {
        if (code != null && projectRepository.existsByCodeIgnoreCase(code)) {
            throw new ProjectCodeAlreadyExistsException(code);
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidProjectDatesException();
        }
    }

    private void validateManagers(UUID projectManagerId, UUID deliveryManagerId) {
        List<UUID> managers = new ArrayList<>();

        if (projectManagerId != null) {
            managers.add(projectManagerId);
        }
        if (deliveryManagerId != null) {
            managers.add(deliveryManagerId);
        }

        if (!managers.isEmpty()) {
            employeeDirectoryService.requireAll(managers);
        }
    }

    private void validateTransition(ProjectStatus current, ProjectStatus target) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidProjectStatusTransitionException(current, target);
        }
    }

    private void verifyVersion(Long expectedVersion, Long actualVersion) {
        if (expectedVersion != null && !expectedVersion.equals(actualVersion)) {
            throw new ObjectOptimisticLockingFailureException(Project.class, expectedVersion);
        }
    }

    private Map<String, Object> projectPayload(Project project) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", project.getId());
        payload.put("code", project.getCode());
        payload.put("name", project.getName());
        payload.put("status", project.getStatus());
        payload.put("startDate", project.getStartDate());
        payload.put("endDate", project.getEndDate());
        payload.put("projectManagerId", project.getProjectManagerId());
        payload.put("deliveryManagerId", project.getDeliveryManagerId());

        return payload;
    }

    private Map<String, Object> statusPayload(Project project, ProjectStatus previousStatus) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", project.getId());
        payload.put("code", project.getCode());
        payload.put("previousStatus", previousStatus);
        payload.put("status", project.getStatus());

        return payload;
    }
}
