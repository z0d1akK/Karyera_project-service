package com.zodiakk.projectservice.project.service;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.project.dto.request.action.ProjectStatusUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ProjectService {

    Page<ProjectResponseDto> findAll(ProjectFilterDto filter, PageRequestDto pageRequest);

    ProjectResponseDto getById(UUID id);

    ProjectResponseDto create(ProjectCreateRequestDto request);

    ProjectResponseDto update(UUID id, ProjectUpdateRequestDto request);

    ProjectResponseDto changeStatus(UUID id, ProjectStatusUpdateRequestDto request);
}
