package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.project.dto.request.create.ProjectLinkCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectLinkUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectLinkResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectLink;
import com.zodiakk.projectservice.project.exception.link.ProjectLinkNotFoundException;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectLinkMapper;
import com.zodiakk.projectservice.project.repository.ProjectLinkRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.project.service.ProjectLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectLinkServiceImpl implements ProjectLinkService {

    private final ProjectLinkRepository projectLinkRepository;

    private final ProjectRepository projectRepository;

    private final ProjectLinkMapper projectLinkMapper;

    @Override
    @PreAuthorize("@projectAccess.canViewProject(#projectId)")
    public List<ProjectLinkResponseDto> findAll(UUID projectId) {
        return projectLinkRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(projectLinkMapper::toResponseDto)
                .toList();
    }

    @Override
    @PreAuthorize("@projectAccess.canEditProject(#projectId)")
    @Transactional
    public ProjectLinkResponseDto create(UUID projectId, ProjectLinkCreateRequestDto request) {
        request.setProjectId(projectId);

        if (!projectRepository.existsById(request.getProjectId())) {
            throw new ProjectNotFoundException(request.getProjectId());
        }

        ProjectLink link = projectLinkMapper.toEntity(request);

        return projectLinkMapper.toResponseDto(projectLinkRepository.save(link));
    }

    @Override
    @PreAuthorize("@projectAccess.canEditLink(#id)")
    @Transactional
    public ProjectLinkResponseDto update(UUID id, ProjectLinkUpdateRequestDto request) {
        ProjectLink link = findById(id);

        projectLinkMapper.updateEntity(request, link);

        return projectLinkMapper.toResponseDto(projectLinkRepository.save(link));
    }

    @Override
    @PreAuthorize("@projectAccess.canEditLink(#id)")
    @Transactional
    public void delete(UUID id) {
        projectLinkRepository.delete(findById(id));
    }

    private ProjectLink findById(UUID id) {
        return projectLinkRepository.findById(id)
                .orElseThrow(() -> new ProjectLinkNotFoundException(id));
    }
}
