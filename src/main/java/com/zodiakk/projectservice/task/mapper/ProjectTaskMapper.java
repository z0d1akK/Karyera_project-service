package com.zodiakk.projectservice.task.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.task.dto.request.create.ProjectTaskCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.ProjectTaskUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.ProjectTaskResponseDto;
import com.zodiakk.projectservice.task.entity.ProjectTask;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ProjectTaskMapper {

    ProjectTask toEntity(ProjectTaskCreateRequestDto dto);

    ProjectTaskResponseDto toResponseDto(ProjectTask entity);

    void updateEntity(ProjectTaskUpdateRequestDto dto, @MappingTarget ProjectTask entity);
}
