package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.common.cache.CacheNames;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.TaskTypeCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.filter.TaskTypeFilterDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskTypeUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskTypeResponseDto;
import com.zodiakk.projectservice.task.entity.TaskType;
import com.zodiakk.projectservice.task.exception.type.TaskTypeCodeAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeInUseException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeNotFoundException;
import com.zodiakk.projectservice.task.mapper.TaskTypeMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
import com.zodiakk.projectservice.task.repository.specification.taskType.TaskTypePageableFactory;
import com.zodiakk.projectservice.task.repository.specification.taskType.TaskTypeSpecification;
import com.zodiakk.projectservice.task.service.TaskTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskTypeServiceImpl implements TaskTypeService {

    private final TaskTypeRepository taskTypeRepository;

    private final ProjectTaskRepository projectTaskRepository;

    private final TaskTypeMapper taskTypeMapper;

    @Override
    @PreAuthorize("isAuthenticated()")
    public Page<TaskTypeResponseDto> findAll(TaskTypeFilterDto filter, PageRequestDto pageRequest) {
        Pageable pageable = TaskTypePageableFactory.create(pageRequest);

        return taskTypeRepository
                .findAll(TaskTypeSpecification.filter(filter), pageable)
                .map(taskTypeMapper::toResponseDto);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Cacheable(cacheNames = CacheNames.TASK_TYPES, key = "#id")
    public TaskTypeResponseDto getById(UUID id) {
        return taskTypeMapper.toResponseDto(findById(id));
    }

    @Override
    @PreAuthorize("@projectAccess.canManageCatalogs()")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TASK_TYPES, allEntries = true)
    public TaskTypeResponseDto create(TaskTypeCreateRequestDto request) {
        if (taskTypeRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new TaskTypeCodeAlreadyExistsException(request.getCode());
        }

        TaskType taskType = taskTypeMapper.toEntity(request);
        taskType.setActive(request.getActive() == null || request.getActive());

        return taskTypeMapper.toResponseDto(taskTypeRepository.save(taskType));
    }

    @Override
    @PreAuthorize("@projectAccess.canManageCatalogs()")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TASK_TYPES, allEntries = true)
    public TaskTypeResponseDto update(UUID id, TaskTypeUpdateRequestDto request) {
        TaskType taskType = findById(id);

        if (request.getCode() != null
                && !request.getCode().isBlank()
                && !request.getCode().equalsIgnoreCase(taskType.getCode())
                && taskTypeRepository.existsByCodeIgnoreCaseAndIdNot(request.getCode(), id)) {
            throw new TaskTypeCodeAlreadyExistsException(request.getCode());
        }

        taskTypeMapper.updateEntity(request, taskType);

        return taskTypeMapper.toResponseDto(taskTypeRepository.save(taskType));
    }

    @Override
    @PreAuthorize("@projectAccess.canManageCatalogs()")
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TASK_TYPES, allEntries = true)
    public void delete(UUID id) {
        TaskType taskType = findById(id);

        if (projectTaskRepository.countByTaskTypeId(id) > 0) {
            throw new TaskTypeInUseException(id);
        }

        taskTypeRepository.delete(taskType);
    }

    private TaskType findById(UUID id) {
        return taskTypeRepository.findById(id)
                .orElseThrow(() -> new TaskTypeNotFoundException(id));
    }
}
