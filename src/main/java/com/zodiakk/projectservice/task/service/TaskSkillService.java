package com.zodiakk.projectservice.task.service;

import com.zodiakk.projectservice.task.dto.request.create.TaskSkillCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskSkillResponseDto;

import java.util.List;
import java.util.UUID;

public interface TaskSkillService {

    List<TaskSkillResponseDto> findAll(UUID taskId);

    TaskSkillResponseDto create(UUID taskId, TaskSkillCreateRequestDto request);

    void delete(UUID id);
}
