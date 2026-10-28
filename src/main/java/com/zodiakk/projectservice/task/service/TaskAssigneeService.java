package com.zodiakk.projectservice.task.service;

import com.zodiakk.projectservice.task.dto.request.action.TaskAssigneesUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskAssigneeResponseDto;

import java.util.List;
import java.util.UUID;

public interface TaskAssigneeService {

    List<TaskAssigneeResponseDto> findAll(UUID taskId);

    List<TaskAssigneeResponseDto> replace(UUID taskId, TaskAssigneesUpdateRequestDto request);
}
