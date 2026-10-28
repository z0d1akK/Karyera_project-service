package com.zodiakk.projectservice.project.service;

import com.zodiakk.projectservice.project.dto.request.create.ProjectLinkCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectLinkUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectLinkResponseDto;

import java.util.List;
import java.util.UUID;

public interface ProjectLinkService {

    List<ProjectLinkResponseDto> findAll(UUID projectId);

    ProjectLinkResponseDto create(UUID projectId, ProjectLinkCreateRequestDto request);

    ProjectLinkResponseDto update(UUID id, ProjectLinkUpdateRequestDto request);

    void delete(UUID id);
}
