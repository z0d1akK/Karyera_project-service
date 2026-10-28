package com.zodiakk.projectservice.task.service;

import com.zodiakk.projectservice.task.dto.response.TaskActivityResponseDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TaskActivityService {

    List<TaskActivityResponseDto> findAll(UUID taskId);

    TaskActivityResponseDto record(
            UUID taskId,
            UUID actorEmployeeId,
            String action,
            Map<String, Object> changes
    );
}
