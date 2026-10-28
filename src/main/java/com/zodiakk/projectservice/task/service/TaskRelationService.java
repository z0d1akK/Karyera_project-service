package com.zodiakk.projectservice.task.service;

import com.zodiakk.projectservice.task.dto.request.action.TaskRelationCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskRelationResponseDto;

import java.util.List;
import java.util.UUID;

public interface TaskRelationService {

    List<TaskRelationResponseDto> findAll(UUID taskId);

    TaskRelationResponseDto create(UUID sourceTaskId, TaskRelationCreateRequestDto request);

    void delete(UUID id);
}
