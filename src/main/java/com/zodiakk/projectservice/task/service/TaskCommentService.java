package com.zodiakk.projectservice.task.service;

import com.zodiakk.projectservice.task.dto.request.action.TaskCommentCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskCommentResponseDto;

import java.util.List;
import java.util.UUID;

public interface TaskCommentService {

    List<TaskCommentResponseDto> findAll(UUID taskId);

    TaskCommentResponseDto create(UUID taskId, TaskCommentCreateRequestDto request);
}
