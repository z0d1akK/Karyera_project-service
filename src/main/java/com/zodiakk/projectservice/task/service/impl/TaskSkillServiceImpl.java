package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.task.dto.request.create.TaskSkillCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskSkillResponseDto;
import com.zodiakk.projectservice.task.entity.TaskSkill;
import com.zodiakk.projectservice.task.exception.skill.TaskSkillAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.skill.TaskSkillNotFoundException;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.mapper.TaskSkillMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskSkillRepository;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import com.zodiakk.projectservice.task.service.TaskSkillService;
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
public class TaskSkillServiceImpl implements TaskSkillService {

    private static final String ACTION_ADDED = "TASK_SKILL_ADDED";

    private static final String ACTION_REMOVED = "TASK_SKILL_REMOVED";

    private final TaskSkillRepository taskSkillRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final TaskSkillMapper taskSkillMapper;

    private final TaskActivityService taskActivityService;

    private final EmployeeDirectoryService employeeDirectoryService;

    @Override
    @PreAuthorize("@projectAccess.canViewTask(#taskId)")
    public List<TaskSkillResponseDto> findAll(UUID taskId) {
        if (!projectTaskRepository.existsById(taskId)) {
            throw new ProjectTaskNotFoundException(taskId);
        }

        return taskSkillRepository.findAllByTaskId(taskId)
                .stream()
                .map(taskSkillMapper::toResponseDto)
                .toList();
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTask(#taskId)")
    @Transactional
    public TaskSkillResponseDto create(UUID taskId, TaskSkillCreateRequestDto request) {
        if (!projectTaskRepository.existsById(taskId)) {
            throw new ProjectTaskNotFoundException(taskId);
        }

        if (taskSkillRepository.existsByTaskIdAndSkillId(taskId, request.getSkillId())) {
            throw new TaskSkillAlreadyExistsException(request.getSkillId());
        }

        TaskSkill skill = TaskSkill.builder()
                .taskId(taskId)
                .skillId(request.getSkillId())
                .build();

        TaskSkill saved = taskSkillRepository.save(skill);

        taskActivityService.record(
                taskId,
                employeeDirectoryService.getCurrentEmployee().getEmployeeId(),
                ACTION_ADDED,
                Map.of("skillId", saved.getSkillId())
        );

        return taskSkillMapper.toResponseDto(saved);
    }

    @Override
    @PreAuthorize("@projectAccess.canEditTaskSkill(#id)")
    @Transactional
    public void delete(UUID id) {
        TaskSkill skill = taskSkillRepository.findById(id)
                .orElseThrow(() -> new TaskSkillNotFoundException(id));

        taskSkillRepository.delete(skill);

        taskActivityService.record(
                skill.getTaskId(),
                employeeDirectoryService.getCurrentEmployee().getEmployeeId(),
                ACTION_REMOVED,
                Map.of("skillId", skill.getSkillId())
        );
    }
}
