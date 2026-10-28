package com.zodiakk.projectservice.task.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.task.dto.request.create.TaskAssigneeCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskAssigneeUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskAssigneeResponseDto;
import com.zodiakk.projectservice.task.entity.TaskAssignee;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TaskAssigneeMapper {

    TaskAssignee toEntity(TaskAssigneeCreateRequestDto dto);

    TaskAssigneeResponseDto toResponseDto(TaskAssignee entity);

    void updateEntity(TaskAssigneeUpdateRequestDto dto, @MappingTarget TaskAssignee entity);
}
