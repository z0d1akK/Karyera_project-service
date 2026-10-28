package com.zodiakk.projectservice.project.service;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectRoleCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectRoleFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectRoleUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectRoleResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ProjectRoleService {

    Page<ProjectRoleResponseDto> findAll(ProjectRoleFilterDto filter, PageRequestDto pageRequest);

    ProjectRoleResponseDto getById(UUID id);

    ProjectRoleResponseDto create(ProjectRoleCreateRequestDto request);

    ProjectRoleResponseDto update(UUID id, ProjectRoleUpdateRequestDto request);

    void delete(UUID id);
}
