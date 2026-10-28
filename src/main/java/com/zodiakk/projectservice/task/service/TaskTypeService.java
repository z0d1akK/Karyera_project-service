package com.zodiakk.projectservice.task.service;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.TaskTypeCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.filter.TaskTypeFilterDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskTypeUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskTypeResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface TaskTypeService {

    Page<TaskTypeResponseDto> findAll(TaskTypeFilterDto filter, PageRequestDto pageRequest);

    TaskTypeResponseDto getById(UUID id);

    TaskTypeResponseDto create(TaskTypeCreateRequestDto request);

    TaskTypeResponseDto update(UUID id, TaskTypeUpdateRequestDto request);

    void delete(UUID id);
}
