package com.zodiakk.projectservice.project.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.project.dto.request.create.ProjectCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectResponseDto;
import com.zodiakk.projectservice.project.entity.Project;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ProjectMapper {

    Project toEntity(ProjectCreateRequestDto dto);

    ProjectResponseDto toResponseDto(Project entity);

    void updateEntity(ProjectUpdateRequestDto dto, @MappingTarget Project entity);
}
