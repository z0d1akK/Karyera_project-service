package com.zodiakk.projectservice.project.service;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.project.dto.request.action.ProjectAssignmentEndRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectAssignmentCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectAssignmentFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectAssignmentUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectAssignmentResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ProjectAssignmentService {

    Page<ProjectAssignmentResponseDto> findAll(
            ProjectAssignmentFilterDto filter,
            PageRequestDto pageRequest
    );

    ProjectAssignmentResponseDto create(UUID projectId, ProjectAssignmentCreateRequestDto request);

    ProjectAssignmentResponseDto update(UUID id, ProjectAssignmentUpdateRequestDto request);

    ProjectAssignmentResponseDto end(UUID id, ProjectAssignmentEndRequestDto request);
}
