package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.common.cache.CacheNames;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectRoleCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectRoleFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectRoleUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectRoleResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectRole;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleCodeAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleInUseException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectRoleMapper;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectRoleRepository;
import com.zodiakk.projectservice.project.repository.specification.projectRole.ProjectRolePageableFactory;
import com.zodiakk.projectservice.project.repository.specification.projectRole.ProjectRoleSpecification;
import com.zodiakk.projectservice.project.service.ProjectRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectRoleServiceImpl implements ProjectRoleService {

    private final ProjectRoleRepository projectRoleRepository;

    private final ProjectAssignmentRepository projectAssignmentRepository;

    private final ProjectRoleMapper projectRoleMapper;

    @Override
    @PreAuthorize("isAuthenticated()")
    public Page<ProjectRoleResponseDto> findAll(ProjectRoleFilterDto filter, PageRequestDto pageRequest) {
        Pageable pageable = ProjectRolePageableFactory.create(pageRequest);

        return projectRoleRepository
                .findAll(ProjectRoleSpecification.filter(filter), pageable)
                .map(projectRoleMapper::toResponseDto);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Cacheable(cacheNames = CacheNames.PROJECT_ROLES, key = "#id")
    public ProjectRoleResponseDto getById(UUID id) {
        return projectRoleMapper.toResponseDto(findById(id));
    }

    @Override
    @PreAuthorize("@projectAccess.canManageCatalogs()")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PROJECT_ROLES, allEntries = true)
    public ProjectRoleResponseDto create(ProjectRoleCreateRequestDto request) {
        if (projectRoleRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new ProjectRoleCodeAlreadyExistsException(request.getCode());
        }

        ProjectRole role = projectRoleMapper.toEntity(request);
        role.setActive(request.getActive() == null || request.getActive());

        return projectRoleMapper.toResponseDto(projectRoleRepository.save(role));
    }

    @Override
    @PreAuthorize("@projectAccess.canManageCatalogs()")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PROJECT_ROLES, allEntries = true)
    public ProjectRoleResponseDto update(UUID id, ProjectRoleUpdateRequestDto request) {
        ProjectRole role = findById(id);

        if (request.getCode() != null
                && !request.getCode().isBlank()
                && !request.getCode().equalsIgnoreCase(role.getCode())
                && projectRoleRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new ProjectRoleCodeAlreadyExistsException(request.getCode());
        }

        projectRoleMapper.updateEntity(request, role);

        return projectRoleMapper.toResponseDto(projectRoleRepository.save(role));
    }

    @Override
    @PreAuthorize("@projectAccess.canManageCatalogs()")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PROJECT_ROLES, allEntries = true)
    public void delete(UUID id) {
        ProjectRole role = findById(id);

        if (projectAssignmentRepository.countByProjectRoleId(id) > 0) {
            throw new ProjectRoleInUseException(id);
        }

        projectRoleRepository.delete(role);
    }

    private ProjectRole findById(UUID id) {
        return projectRoleRepository.findById(id)
                .orElseThrow(() -> new ProjectRoleNotFoundException(id));
    }
}
