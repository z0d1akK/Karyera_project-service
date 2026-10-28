package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.task.dto.response.TaskActivityResponseDto;
import com.zodiakk.projectservice.task.entity.TaskActivity;
import com.zodiakk.projectservice.task.mapper.TaskActivityMapper;
import com.zodiakk.projectservice.task.repository.TaskActivityRepository;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskActivityServiceImpl implements TaskActivityService {

    private final TaskActivityRepository taskActivityRepository;

    private final TaskActivityMapper taskActivityMapper;

    private final ObjectMapper objectMapper;

    @Override
    @PreAuthorize("@projectAccess.canViewTask(#taskId)")
    public List<TaskActivityResponseDto> findAll(UUID taskId) {
        return taskActivityRepository.findAllByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(taskActivityMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TaskActivityResponseDto record(
            UUID taskId,
            UUID actorEmployeeId,
            String action,
            Map<String, Object> changes
    ) {
        TaskActivity activity = TaskActivity.builder()
                .taskId(taskId)
                .actorEmployeeId(actorEmployeeId)
                .action(action)
                .changes(changes == null ? null : objectMapper.writeValueAsString(changes))
                .build();

        return taskActivityMapper.toResponseDto(taskActivityRepository.save(activity));
    }
}
