package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectMilestoneCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectMilestoneFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectMilestoneUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectMilestoneResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectMilestone;
import com.zodiakk.projectservice.project.exception.milestone.ProjectMilestoneNotFoundException;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectMilestoneMapper;
import com.zodiakk.projectservice.project.repository.ProjectMilestoneRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.project.repository.specification.projectMilestone.ProjectMilestonePageableFactory;
import com.zodiakk.projectservice.project.repository.specification.projectMilestone.ProjectMilestoneSpecification;
import com.zodiakk.projectservice.project.service.ProjectMilestoneService;
import com.zodiakk.projectservice.security.access.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {

    private final ProjectMilestoneRepository projectMilestoneRepository;

    private final ProjectRepository projectRepository;

    private final ProjectMilestoneMapper projectMilestoneMapper;

    private final ProjectAccessService projectAccessService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public Page<ProjectMilestoneResponseDto> findAll(
            ProjectMilestoneFilterDto filter,
            PageRequestDto pageRequest
    ) {
        if (filter.getProjectId() != null
                && !projectAccessService.canViewProject(filter.getProjectId())) {
            return Page.empty();
        }

        Pageable pageable = ProjectMilestonePageableFactory.create(pageRequest);

        return projectMilestoneRepository
                .findAll(ProjectMilestoneSpecification.filter(filter), pageable)
                .map(projectMilestoneMapper::toResponseDto);
    }

    @Override
    @PreAuthorize("@projectAccess.canEditProject(#projectId)")
    @Transactional
    public ProjectMilestoneResponseDto create(UUID projectId, ProjectMilestoneCreateRequestDto request) {
        request.setProjectId(projectId);

        if (!projectRepository.existsById(request.getProjectId())) {
            throw new ProjectNotFoundException(request.getProjectId());
        }

        ProjectMilestone milestone = projectMilestoneMapper.toEntity(request);

        return projectMilestoneMapper.toResponseDto(
                projectMilestoneRepository.save(milestone)
        );
    }

    @Override
    @PreAuthorize("@projectAccess.canEditMilestone(#id)")
    @Transactional
    public ProjectMilestoneResponseDto update(UUID id, ProjectMilestoneUpdateRequestDto request) {
        ProjectMilestone milestone = findById(id);

        projectMilestoneMapper.updateEntity(request, milestone);

        return projectMilestoneMapper.toResponseDto(
                projectMilestoneRepository.save(milestone)
        );
    }

    @Override
    @PreAuthorize("@projectAccess.canEditMilestone(#id)")
    @Transactional
    public void delete(UUID id) {
        projectMilestoneRepository.delete(findById(id));
    }

    private ProjectMilestone findById(UUID id) {
        return projectMilestoneRepository.findById(id)
                .orElseThrow(() -> new ProjectMilestoneNotFoundException(id));
    }
}
