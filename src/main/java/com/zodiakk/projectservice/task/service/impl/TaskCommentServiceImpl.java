package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.task.dto.request.action.TaskCommentCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskCommentResponseDto;
import com.zodiakk.projectservice.task.entity.TaskComment;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.mapper.TaskCommentMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskCommentRepository;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import com.zodiakk.projectservice.task.service.TaskCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskCommentServiceImpl implements TaskCommentService {

    private static final String ACTION_COMMENT_ADDED = "COMMENT_ADDED";

    private final TaskCommentRepository taskCommentRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final TaskCommentMapper taskCommentMapper;

    private final TaskActivityService taskActivityService;

    private final EmployeeDirectoryService employeeDirectoryService;

    @Override
    @PreAuthorize("@projectAccess.canViewTask(#taskId)")
    public List<TaskCommentResponseDto> findAll(UUID taskId) {
        if (!projectTaskRepository.existsById(taskId)) {
            throw new ProjectTaskNotFoundException(taskId);
        }

        return taskCommentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(taskCommentMapper::toResponseDto)
                .toList();
    }

    @Override
    @PreAuthorize("@projectAccess.canViewTask(#taskId)")
    @Transactional
    public TaskCommentResponseDto create(UUID taskId, TaskCommentCreateRequestDto request) {
        if (!projectTaskRepository.existsById(taskId)) {
            throw new ProjectTaskNotFoundException(taskId);
        }

        UUID authorEmployeeId = employeeDirectoryService.getCurrentEmployee().getEmployeeId();

        TaskComment comment = TaskComment.builder()
                .taskId(taskId)
                .authorEmployeeId(authorEmployeeId)
                .content(request.getContent())
                .build();

        TaskComment saved = taskCommentRepository.save(comment);

        taskActivityService.record(
                taskId,
                authorEmployeeId,
                ACTION_COMMENT_ADDED,
                Map.of("commentId", saved.getId())
        );

        return taskCommentMapper.toResponseDto(saved);
    }
}
