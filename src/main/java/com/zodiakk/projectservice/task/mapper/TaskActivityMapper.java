package com.zodiakk.projectservice.task.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.task.dto.request.create.TaskActivityCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskActivityUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskActivityResponseDto;
import com.zodiakk.projectservice.task.entity.TaskActivity;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TaskActivityMapper {

    TaskActivity toEntity(TaskActivityCreateRequestDto dto);

    TaskActivityResponseDto toResponseDto(TaskActivity entity);

    void updateEntity(TaskActivityUpdateRequestDto dto, @MappingTarget TaskActivity entity);
}
