package com.zodiakk.projectservice.project.service;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectMilestoneCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectMilestoneFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectMilestoneUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectMilestoneResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ProjectMilestoneService {

    Page<ProjectMilestoneResponseDto> findAll(
            ProjectMilestoneFilterDto filter,
            PageRequestDto pageRequest
    );

    ProjectMilestoneResponseDto create(UUID projectId, ProjectMilestoneCreateRequestDto request);

    ProjectMilestoneResponseDto update(UUID id, ProjectMilestoneUpdateRequestDto request);

    void delete(UUID id);
}
