package com.zodiakk.projectservice.task.service;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskBoardPositionUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskParentUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.ProjectTaskCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.filter.ProjectTaskFilterDto;
import com.zodiakk.projectservice.task.dto.request.update.ProjectTaskUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.ProjectTaskResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ProjectTaskService {

    Page<ProjectTaskResponseDto> findAll(
            UUID projectId,
            ProjectTaskFilterDto filter,
            PageRequestDto pageRequest
    );

    ProjectTaskResponseDto getById(UUID id);

    ProjectTaskResponseDto create(UUID projectId, ProjectTaskCreateRequestDto request);

    ProjectTaskResponseDto update(UUID id, ProjectTaskUpdateRequestDto request);

    void archive(UUID id);

    List<ProjectTaskResponseDto> findChildren(UUID id);

    ProjectTaskResponseDto changeParent(UUID id, TaskParentUpdateRequestDto request);

    ProjectTaskResponseDto move(UUID id, TaskBoardPositionUpdateRequestDto request);
}
