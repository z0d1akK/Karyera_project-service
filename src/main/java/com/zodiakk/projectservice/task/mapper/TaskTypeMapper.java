package com.zodiakk.projectservice.task.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.task.dto.request.create.TaskTypeCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskTypeUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskTypeResponseDto;
import com.zodiakk.projectservice.task.entity.TaskType;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TaskTypeMapper {

    TaskType toEntity(TaskTypeCreateRequestDto dto);

    TaskTypeResponseDto toResponseDto(TaskType entity);

    void updateEntity(TaskTypeUpdateRequestDto dto, @MappingTarget TaskType entity);
}
